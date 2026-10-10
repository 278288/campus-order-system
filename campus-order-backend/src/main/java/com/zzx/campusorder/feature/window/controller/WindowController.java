package com.zzx.campusorder.feature.window.controller;

import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.window.dto.WindowSaveRequest;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.service.WindowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 食堂窗口接口：管理员维护窗口。
 * 面向未登录用户的只读浏览接口位于 {@code /api/public/windows}。
 */
@RestController
@RequestMapping("/api/windows")
@RequiredArgsConstructor
public class WindowController {

    private final WindowService windowService;

    /** 查询窗口列表，供登录用户在管理页面使用 */
    @GetMapping
    public Result<List<CanteenWindow>> list(@RequestParam(required = false) Integer businessStatus,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(windowService.list(businessStatus, status));
    }

    /** 查询窗口详情 */
    @GetMapping("/{id}")
    public Result<CanteenWindow> detail(@PathVariable Long id) {
        return Result.success(windowService.getById(id));
    }

    /** 新增窗口，仅管理员可调用 */
    @PostMapping
    public Result<CanteenWindow> create(@Valid @RequestBody WindowSaveRequest request) {
        return Result.success("窗口新增成功", windowService.create(request));
    }

    /** 修改窗口信息，仅管理员可调用 */
    @PutMapping("/{id}")
    public Result<CanteenWindow> update(@PathVariable Long id, @Valid @RequestBody WindowSaveRequest request) {
        return Result.success("窗口修改成功", windowService.update(id, request));
    }

    /** 商家维护本人窗口的营业状态：1 营业中，0 已打烊。打烊只阻止新订单，不影响已有订单履约 */
    @PutMapping("/{id}/business-status")
    public Result<CanteenWindow> changeBusinessStatus(@PathVariable Long id, @RequestParam Integer businessStatus) {
        return Result.success("营业状态已更新", windowService.changeBusinessStatus(id, businessStatus));
    }

    /** 停用或启用窗口，仅管理员可调用。存在历史数据时只停用，不做物理删除 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        windowService.changeStatus(id, status);
        return Result.success("窗口状态已更新", null);
    }
}
