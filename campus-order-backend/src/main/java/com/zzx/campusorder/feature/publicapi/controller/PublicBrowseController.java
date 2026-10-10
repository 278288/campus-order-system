package com.zzx.campusorder.feature.publicapi.controller;

import com.zzx.campusorder.common.result.PageResult;
import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.dish.service.DishService;
import com.zzx.campusorder.feature.dish.vo.DishVO;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.service.WindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 只读公开接口：未登录用户即可浏览营业中的食堂窗口与在售菜品。
 *
 * <p>该命名空间下只提供查询能力，不含任何写操作，因此在鉴权拦截器白名单中整体放行，
 * 从命名空间层面避免“查询放行导致写接口一并放行”的问题。需要登录的对应接口仍保留在
 * /api/windows 与 /api/dishes 下。</p>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBrowseController {

    private final WindowService windowService;
    private final DishService dishService;

    /** 查询食堂窗口列表，可按营业状态与记录状态过滤 */
    @GetMapping("/windows")
    public Result<List<CanteenWindow>> windows(@RequestParam(required = false) Integer businessStatus,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(windowService.list(businessStatus, status));
    }

    /** 查询窗口详情 */
    @GetMapping("/windows/{id}")
    public Result<CanteenWindow> windowDetail(@PathVariable Long id) {
        return Result.success(windowService.getById(id));
    }

    /** 分页浏览菜品，支持窗口、名称、分类过滤 */
    @GetMapping("/dishes")
    public Result<PageResult<DishVO>> dishes(@RequestParam(required = false) Long windowId,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Boolean onlyOnSale,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(dishService.pageOnSale(windowId, keyword, category, onlyOnSale, pageNum, pageSize));
    }

    /** 查询菜品详情 */
    @GetMapping("/dishes/{id:\\d+}")
    public Result<DishVO> dishDetail(@PathVariable Long id) {
        return Result.success(dishService.getDetail(id));
    }

    /** 查询全部菜品分类，供筛选下拉框使用 */
    @GetMapping("/dishes/categories")
    public Result<List<String>> categories() {
        return Result.success(dishService.listCategories());
    }
}
