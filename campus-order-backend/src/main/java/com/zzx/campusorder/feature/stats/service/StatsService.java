package com.zzx.campusorder.feature.stats.service;

import com.zzx.campusorder.common.exception.BusinessException;
import com.zzx.campusorder.common.security.Roles;
import com.zzx.campusorder.common.security.UserContext;
import com.zzx.campusorder.feature.order.entity.Order;
import com.zzx.campusorder.feature.order.entity.OrderStatus;
import com.zzx.campusorder.feature.stats.mapper.StatsMapper;
import com.zzx.campusorder.feature.stats.vo.OrderItemStatVO;
import com.zzx.campusorder.feature.stats.vo.StatsVO;
import com.zzx.campusorder.feature.window.entity.CanteenWindow;
import com.zzx.campusorder.feature.window.service.WindowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营统计业务：按日期统计有效订单与金额、热销菜品排行。
 * 商家只能查看本人所属窗口，管理员可查看全平台或指定窗口。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_RANGE_DAYS = 92;

    private final StatsMapper statsMapper;
    private final WindowService windowService;

    /**
     * 订单与金额统计。
     *
     * @param startDate 起始日期，格式 yyyy-MM-dd，为空时默认最近 7 天
     * @param endDate   结束日期，格式 yyyy-MM-dd，为空时默认今天
     * @param windowId  窗口编号；商家忽略该参数并使用本人窗口，管理员为空时统计全平台
     */
    public StatsVO orderStats(String startDate, String endDate, Long windowId) {
        LocalDate end = parseDate(endDate, LocalDate.now());
        LocalDate start = parseDate(startDate, end.minusDays(6));
        if (start.isAfter(end)) {
            throw BusinessException.badRequest("起始日期不能晚于结束日期");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw BusinessException.badRequest("统计区间不能超过 " + MAX_RANGE_DAYS + " 天");
        }

        Long effectiveWindowId;
        String scope;
        if (Roles.MERCHANT.equals(UserContext.requireRole())) {
            CanteenWindow window = windowService.requireMerchantWindow();
            effectiveWindowId = window.getId();
            scope = "window";
        } else {
            Roles.requireAdmin();
            effectiveWindowId = windowId;
            scope = windowId == null ? "all" : "window";
        }

        List<Order> orders = statsMapper.selectOrdersBetween(start, end, effectiveWindowId);

        long validCount = 0L;
        long finishedCount = 0L;
        long canceledCount = 0L;
        BigDecimal validAmount = BigDecimal.ZERO;
        BigDecimal finishedAmount = BigDecimal.ZERO;
        Map<LocalDate, StatsVO.DailyStat> dailyMap = new LinkedHashMap<>();

        for (Order order : orders) {
            LocalDate day = order.getCreateTime().toLocalDate();
            boolean canceled = order.getStatus() != null
                    && (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REJECTED);
            BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

            if (canceled) {
                canceledCount++;
                continue;
            }
            validCount++;
            validAmount = validAmount.add(amount);
            if (order.getStatus() != null && order.getStatus() == OrderStatus.FINISHED) {
                finishedCount++;
                finishedAmount = finishedAmount.add(amount);
            }
            StatsVO.DailyStat daily = dailyMap.computeIfAbsent(day, d -> {
                StatsVO.DailyStat stat = new StatsVO.DailyStat();
                stat.setDate(d.format(DATE_FORMAT));
                stat.setOrderCount(0L);
                stat.setAmount(BigDecimal.ZERO);
                return stat;
            });
            daily.setOrderCount(daily.getOrderCount() + 1);
            daily.setAmount(daily.getAmount().add(amount));
        }

        StatsVO vo = new StatsVO();
        vo.setScope(scope);
        vo.setStartDate(start.format(DATE_FORMAT));
        vo.setEndDate(end.format(DATE_FORMAT));
        vo.setValidOrderCount(validCount);
        vo.setFinishedOrderCount(finishedCount);
        vo.setCanceledOrderCount(canceledCount);
        vo.setValidAmount(validAmount.setScale(2, RoundingMode.HALF_UP));
        vo.setFinishedAmount(finishedAmount.setScale(2, RoundingMode.HALF_UP));
        vo.setAverageAmount(validCount == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : validAmount.divide(BigDecimal.valueOf(validCount), 2, RoundingMode.HALF_UP));
        vo.setDailyStats(new ArrayList<>(dailyMap.values()));

        List<OrderItemStatVO> ranking = statsMapper.selectDishRanking(start, end, effectiveWindowId);
        vo.setDishRanking(ranking.stream().map(r -> {
            StatsVO.DishRank rank = new StatsVO.DishRank();
            rank.setDishId(r.getDishId());
            rank.setDishName(r.getDishName());
            rank.setQuantity(r.getQuantity());
            rank.setAmount(r.getAmount());
            return rank;
        }).toList());
        return vo;
    }

    private LocalDate parseDate(String text, LocalDate defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("日期格式不正确，应为 yyyy-MM-dd：" + text);
        }
    }
}
