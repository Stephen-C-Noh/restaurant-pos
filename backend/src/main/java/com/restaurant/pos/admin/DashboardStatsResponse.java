package com.restaurant.pos.admin;

import com.restaurant.pos.order.OrderStatus;
import com.restaurant.pos.order.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardStatsResponse(
        BigDecimal todayRevenue,
        long todayOrderCount,
        long activeOrderCount,
        BigDecimal avgOrderValue,
        BigDecimal avgRevenuePerTable,
        List<HourlyOrderCount> ordersByHour,
        List<TopSellingItem> topItems,
        List<RecentOrderSummary> recentOrders
) {
    public record HourlyOrderCount(int hour, String label, long orderCount) {}

    public record TopSellingItem(String name, long quantitySold, BigDecimal revenue) {}

    public record RecentOrderSummary(
            String orderNumber,
            OrderType orderType,
            OrderStatus status,
            BigDecimal total,
            Instant createdAt,
            UUID tableId
    ) {}
}
