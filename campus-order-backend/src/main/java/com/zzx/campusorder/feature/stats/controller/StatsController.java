package com.zzx.campusorder.feature.stats.controller;

import com.zzx.campusorder.common.result.Result;
import com.zzx.campusorder.feature.stats.service.StatsService;
import com.zzx.campusorder.feature.stats.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计报表接口。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 按日期统计有效订单与金额，并返回热销菜品排行 */
    @GetMapping("/orders")
    public Result<StatsVO> orderStats(@RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate,
                                      @RequestParam(required = false) Long windowId) {
        return Result.success(statsService.orderStats(startDate, endDate, windowId));
    }
}
