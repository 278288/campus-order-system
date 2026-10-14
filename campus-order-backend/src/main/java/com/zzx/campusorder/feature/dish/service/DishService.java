package com.zzx.campusorder.feature.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.SecurityUtils;
import com.zzx.campusorder.common.security.DbUserDetails;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.feature.dish.dto.DishSaveRequest;
import com.zzx.campusorder.feature.dish.entity.Dish;
import com.zzx.campusorder.feature.dish.mapper.DishMapper;
import com.zzx.campusorder.feature.dish.vo.DishVO;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.mapper.CanteenWindowMapper;
import com.zzx.campusorder.feature.window.service.WindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品业务：公开浏览、商家维护本窗口菜品。
 */
@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final CanteenWindowMapper windowMapper;
    private final WindowService windowService;

    /**
     * 分页浏览在售菜品。师生用户选餐入口，无需登录。
     *
     * @param windowId 窗口编号，为空时返回全部营业中窗口的菜品
     * @param keyword  菜品名称模糊搜索
     * @param category 分类筛选
     * @param onlyOnSale 是否只返回在售菜品，默认 true
     */
    public PageResult<DishVO> pageOnSale(Long windowId, String keyword, String category,
                                         Boolean onlyOnSale, long pageNum, long pageSize) {
        Page<Dish> page = new Page<>(pageNum, pageSize);
        boolean onSaleOnly = !Boolean.FALSE.equals(onlyOnSale);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(windowId != null, Dish::getWindowId, windowId)
                .eq(onSaleOnly, Dish::getStatus, 1)
                .like(StringUtils.hasText(keyword), Dish::getDishName, keyword)
                .eq(StringUtils.hasText(category), Dish::getCategory, category)
                .orderByAsc(Dish::getWindowId)
                .orderByAsc(Dish::getId);
        if (windowId == null) {
            // 未指定窗口时，只返回有效且营业中窗口的菜品，避免展示已停用窗口的菜品
            wrapper.inSql(Dish::getWindowId,
                    "SELECT id FROM canteen_window WHERE status = 1 AND business_status = 1");
        }
        Page<Dish> result = dishMapper.selectPage(page, wrapper);
        List<DishVO> records = toVOList(result.getRecords());
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    /**
     * 查询菜品详情。
     */
    public DishVO getDetail(Long id) {
        Dish dish = requireDish(id);
        return toVO(dish, windowMapper.selectById(dish.getWindowId()));
    }

    /**
     * 商家查询本人窗口的全部菜品，包含已下架菜品（用于维护列表）。
     * 该接口面向商家，必须校验角色，否则师生用户也能读到商家维护视图所需的数据。
     */
    public List<DishVO> listMyWindowDishes(Integer status) {
        if (!SecurityUtils.hasRoleCode(Roles.CODE_MERCHANT)) {
            throw BusinessException.forbidden("该操作仅食堂商家可执行");
        }
        CanteenWindow window = windowService.requireMerchantWindow();
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getWindowId, window.getId())
                .eq(status != null, Dish::getStatus, status)
                .orderByAsc(Dish::getId));
        return toVOList(dishes);
    }

    /**
     * 商家新增菜品。窗口由服务端根据登录身份判定，前端传入的窗口一律忽略，防止越权写他人窗口。
     */
    @Transactional(rollbackFor = Exception.class)
    public DishVO create(DishSaveRequest request) {
        CanteenWindow window = windowService.requireMerchantWindow();
        ensurePriceAndStock(request);
        Dish dish = new Dish();
        dish.setWindowId(window.getId());
        applyRequest(dish, request);
        dishMapper.insert(dish);
        return toVO(dish, window);
    }

    /**
     * 商家修改本窗口菜品。
     */
    @Transactional(rollbackFor = Exception.class)
    public DishVO update(Long id, DishSaveRequest request) {
        CanteenWindow window = windowService.requireMerchantWindow();
        Dish dish = requireDish(id);
        if (!dish.getWindowId().equals(window.getId())) {
            throw BusinessException.forbidden("只能维护本人所属窗口的菜品");
        }
        ensurePriceAndStock(request);
        applyRequest(dish, request);
        dishMapper.updateById(dish);
        return toVO(dish, window);
    }

    /**
     * 商家上架或下架菜品。下架不影响历史订单中的名称与价格快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public DishVO changeStatus(Long id, Integer status) {
        CanteenWindow window = windowService.requireMerchantWindow();
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("上下架状态只能是 0（下架）或 1（在售）");
        }
        Dish dish = requireDish(id);
        if (!dish.getWindowId().equals(window.getId())) {
            throw BusinessException.forbidden("只能维护本人所属窗口的菜品");
        }
        dish.setStatus(status);
        dishMapper.updateById(dish);
        return toVO(dish, window);
    }

    /**
     * 商家调整库存（补货或盘点）。
     */
    @Transactional(rollbackFor = Exception.class)
    public DishVO updateStock(Long id, Integer stock) {
        CanteenWindow window = windowService.requireMerchantWindow();
        if (stock == null || stock < 0) {
            throw BusinessException.badRequest("库存必须为非负整数");
        }
        Dish dish = requireDish(id);
        if (!dish.getWindowId().equals(window.getId())) {
            throw BusinessException.forbidden("只能维护本人所属窗口的菜品");
        }
        dish.setStock(stock);
        dishMapper.updateById(dish);
        return toVO(dish, window);
    }

    private void ensurePriceAndStock(DishSaveRequest request) {
        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw BusinessException.badRequest("菜品价格必须大于 0");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw BusinessException.badRequest("菜品库存必须为非负整数");
        }
    }

    private void applyRequest(Dish dish, DishSaveRequest request) {
        dish.setDishName(request.getDishName());
        dish.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory() : "其他");
        dish.setPrice(request.getPrice());
        dish.setStock(request.getStock());
        dish.setDescription(request.getDescription());
        dish.setImageUrl(request.getImageUrl());
        dish.setStatus(request.getStatus());
    }

    private Dish requireDish(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw BusinessException.notFound("菜品不存在：" + id);
        }
        return dish;
    }

    public List<DishVO> toVOList(List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return List.of();
        }
        List<Long> windowIds = dishes.stream().map(Dish::getWindowId).distinct().toList();
        Map<Long, CanteenWindow> windowMap = new HashMap<>();
        if (!windowIds.isEmpty()) {
            windowMap = windowMapper.selectBatchIds(windowIds).stream()
                    .collect(Collectors.toMap(CanteenWindow::getId, Function.identity()));
        }
        Map<Long, CanteenWindow> finalMap = windowMap;
        return dishes.stream().map(d -> toVO(d, finalMap.get(d.getWindowId()))).toList();
    }

    public DishVO toVO(Dish dish, CanteenWindow window) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setWindowId(dish.getWindowId());
        vo.setDishName(dish.getDishName());
        vo.setCategory(dish.getCategory());
        vo.setPrice(dish.getPrice());
        vo.setStock(dish.getStock());
        vo.setImageUrl(dish.getImageUrl());
        vo.setDescription(dish.getDescription());
        vo.setStatus(dish.getStatus());
        if (window != null) {
            vo.setWindowName(window.getWindowName());
            vo.setWindowBusinessStatus(window.getBusinessStatus());
        }
        boolean onSale = dish.getStatus() != null && dish.getStatus() == 1;
        boolean hasStock = dish.getStock() != null && dish.getStock() > 0;
        boolean windowOpen = window == null || (window.getBusinessStatus() != null && window.getBusinessStatus() == 1
                && window.getStatus() != null && window.getStatus() == 1);
        vo.setPurchasable(onSale && hasStock && windowOpen);
        return vo;
    }

    /**
     * 查询全部菜品分类，供前端筛选下拉框使用。
     */
    public List<String> listCategories() {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>().select(Dish::getCategory))
                .stream().map(Dish::getCategory).filter(StringUtils::hasText).distinct().sorted().toList();
    }

    /**
     * 商家维护菜品时校验窗口归属，供其他模块复用。
     */
    public boolean isOwnWindow(Long windowId) {
        DbUserDetails details = SecurityUtils.currentUserOrNull();
        // 权限注解已保证调用方持有 dish:* 权限；此处额外校验窗口归属，防止跨窗口操作
        if (details == null || !SecurityUtils.hasRoleCode(Roles.CODE_MERCHANT)) {
            return false;
        }
        return windowService.belongsToMerchant(windowId, details.getUserId());
    }
}
