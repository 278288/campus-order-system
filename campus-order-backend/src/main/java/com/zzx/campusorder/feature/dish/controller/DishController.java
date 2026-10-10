package com.zzx.campusorder.feature.dish.controller;

import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.dish.dto.DishSaveRequest;
import com.zzx.campusorder.feature.dish.service.DishService;
import com.zzx.campusorder.feature.dish.vo.DishVO;
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
 * 菜品接口：商家维护本窗口菜品。
 * 面向师生用户的只读浏览接口位于 {@code /api/public/dishes}，未登录即可访问。
 */
@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /** 商家查询本人窗口的全部菜品，包含已下架菜品 */
    @GetMapping("/mine")
    public Result<List<DishVO>> mine(@RequestParam(required = false) Integer status) {
        return Result.success(dishService.listMyWindowDishes(status));
    }

    /** 商家新增菜品，窗口由服务端根据登录身份判定 */
    @PostMapping
    public Result<DishVO> create(@Valid @RequestBody DishSaveRequest request) {
        return Result.success("菜品新增成功", dishService.create(request));
    }

    /** 商家修改本窗口菜品 */
    @PutMapping("/{id}")
    public Result<DishVO> update(@PathVariable Long id, @Valid @RequestBody DishSaveRequest request) {
        return Result.success("菜品修改成功", dishService.update(id, request));
    }

    /** 商家上架或下架菜品 */
    @PutMapping("/{id}/status")
    public Result<DishVO> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.success("菜品状态已更新", dishService.changeStatus(id, status));
    }

    /** 商家调整菜品库存 */
    @PutMapping("/{id}/stock")
    public Result<DishVO> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        return Result.success("库存已更新", dishService.updateStock(id, stock));
    }
}
