package com.zzx.campusorder.feature.window.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.UserContext;
import com.zzx.campusorder.feature.dish.entity.Dish;
import com.zzx.campusorder.feature.dish.mapper.DishMapper;
import com.zzx.campusorder.feature.order.entity.Order;
import com.zzx.campusorder.feature.order.entity.OrderStatus;
import com.zzx.campusorder.feature.order.mapper.OrderMapper;
import com.zzx.campusorder.feature.window.dto.WindowSaveRequest;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.entity.WindowManager;
import com.zzx.campusorder.feature.window.mapper.CanteenWindowMapper;
import com.zzx.campusorder.feature.window.mapper.WindowManagerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 食堂窗口业务：管理员维护窗口，商家查询本人所属窗口。
 */
@Service
@RequiredArgsConstructor
public class WindowService {

    private final CanteenWindowMapper windowMapper;
    private final WindowManagerMapper windowManagerMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;

    /**
     * 查询窗口列表。无需登录，师生用户选餐时使用；status 为空时只返回有效窗口。
     */
    public List<CanteenWindow> list(Integer businessStatus, Integer status) {
        LambdaQueryWrapper<CanteenWindow> wrapper = new LambdaQueryWrapper<CanteenWindow>()
                .eq(status != null, CanteenWindow::getStatus, status)
                .eq(businessStatus != null, CanteenWindow::getBusinessStatus, businessStatus)
                .orderByAsc(CanteenWindow::getId);
        return windowMapper.selectList(wrapper);
    }

    public CanteenWindow getById(Long id) {
        CanteenWindow window = windowMapper.selectById(id);
        if (window == null) {
            throw BusinessException.notFound("窗口不存在：" + id);
        }
        return window;
    }

    @Transactional(rollbackFor = Exception.class)
    public CanteenWindow create(WindowSaveRequest request) {
        Roles.requireAdmin();
        Long count = windowMapper.selectCount(new LambdaQueryWrapper<CanteenWindow>()
                .eq(CanteenWindow::getWindowName, request.getWindowName()));
        if (count != null && count > 0) {
            throw BusinessException.conflict("窗口名称已存在：" + request.getWindowName());
        }
        CanteenWindow window = new CanteenWindow();
        window.setWindowName(request.getWindowName());
        window.setLocation(request.getLocation());
        window.setBusinessStatus(request.getBusinessStatus());
        window.setStatus(1);
        windowMapper.insert(window);
        return window;
    }

    /**
     * 修改窗口信息。营业状态只影响新订单，已有订单继续履约。
     */
    @Transactional(rollbackFor = Exception.class)
    public CanteenWindow update(Long id, WindowSaveRequest request) {
        Roles.requireAdmin();
        CanteenWindow exists = getById(id);
        Long count = windowMapper.selectCount(new LambdaQueryWrapper<CanteenWindow>()
                .eq(CanteenWindow::getWindowName, request.getWindowName())
                .ne(CanteenWindow::getId, id));
        if (count != null && count > 0) {
            throw BusinessException.conflict("窗口名称已被其他窗口使用：" + request.getWindowName());
        }
        exists.setWindowName(request.getWindowName());
        exists.setLocation(request.getLocation());
        exists.setBusinessStatus(request.getBusinessStatus());
        windowMapper.updateById(exists);
        return exists;
    }

    /**
     * 商家维护本人所属窗口的营业状态。打烊只阻止新订单，不影响已有订单继续履约。
     */
    @Transactional(rollbackFor = Exception.class)
    public CanteenWindow changeBusinessStatus(Long id, Integer businessStatus) {
        if (businessStatus == null || (businessStatus != 0 && businessStatus != 1)) {
            throw BusinessException.badRequest("营业状态只能是 0（打烊）或 1（营业中）");
        }
        CanteenWindow window = getById(id);
        UserContext.LoginUser loginUser = UserContext.get();
        boolean isAdmin = loginUser != null && Roles.ADMIN.equals(loginUser.getRole());
        boolean isOwner = loginUser != null && Roles.MERCHANT.equals(loginUser.getRole())
                && belongsToMerchant(id, loginUser.getUserId());
        if (!isAdmin && !isOwner) {
            throw BusinessException.forbidden("只能维护本人所属窗口的营业状态");
        }
        window.setBusinessStatus(businessStatus);
        windowMapper.updateById(window);
        return window;
    }

    /**
     * 停用或启用窗口。存在历史订单或菜品时只做停用，不做物理删除，保留关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        Roles.requireAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("窗口状态只能是 0（停用）或 1（启用）");
        }
        CanteenWindow window = getById(id);
        if (status == 0) {
            // 停用时同时打烊；有历史订单或菜品也只做停用，避免破坏关联
            window.setStatus(0);
            window.setBusinessStatus(0);
        } else {
            window.setStatus(1);
        }
        windowMapper.updateById(window);
    }

    /**
     * 查询当前商家负责的窗口；一个商家可能负责多个窗口，取第一个作为默认操作对象。
     */
    public CanteenWindow requireMerchantWindow() {
        Roles.requireMerchant();
        Long userId = UserContext.requireUserId();
        List<WindowManager> relations = windowManagerMapper.selectList(new LambdaQueryWrapper<WindowManager>()
                .eq(WindowManager::getUserId, userId)
                .orderByAsc(WindowManager::getId));
        if (relations.isEmpty()) {
            throw BusinessException.forbidden("当前商家账号未绑定任何食堂窗口，请联系管理员");
        }
        return getById(relations.get(0).getWindowId());
    }

    /**
     * 查询当前商家负责的全部窗口编号。
     */
    public List<Long> listMerchantWindowIds(Long userId) {
        return windowManagerMapper.selectList(new LambdaQueryWrapper<WindowManager>()
                        .eq(WindowManager::getUserId, userId))
                .stream().map(WindowManager::getWindowId).toList();
    }

    /**
     * 判断指定窗口是否属于该商家，用于越权校验。
     */
    public boolean belongsToMerchant(Long windowId, Long userId) {
        Long count = windowManagerMapper.selectCount(new LambdaQueryWrapper<WindowManager>()
                .eq(WindowManager::getWindowId, windowId)
                .eq(WindowManager::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 重新绑定商家的窗口。管理员调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindMerchantWindow(Long userId, Long windowId) {
        windowManagerMapper.delete(new LambdaQueryWrapper<WindowManager>()
                .eq(WindowManager::getUserId, userId));
        if (windowId != null && windowId > 0) {
            getById(windowId);
            WindowManager relation = new WindowManager();
            relation.setUserId(userId);
            relation.setWindowId(windowId);
            windowManagerMapper.insert(relation);
        }
    }

    /**
     * 统计某窗口未完成的订单数量，窗口停用前提示用。
     */
    public long countActiveOrders(Long windowId) {
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getWindowId, windowId)
                .in(Order::getStatus, OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.READY));
        return count == null ? 0L : count;
    }
}
