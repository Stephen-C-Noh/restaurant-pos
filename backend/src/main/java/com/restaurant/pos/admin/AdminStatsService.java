package com.restaurant.pos.admin;

import com.restaurant.pos.order.Order;
import com.restaurant.pos.order.OrderItemRepository;
import com.restaurant.pos.order.OrderRepository;
import com.restaurant.pos.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        // Same 8 AM cutoff logic as OrderService.createOrder()
        LocalDate today = LocalDate.now();
        if (LocalTime.now().isBefore(LocalTime.of(8, 0))) {
            today = today.minusDays(1);
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = today.atTime(8, 0).atZone(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atTime(8, 0).atZone(zone).toInstant();

        // Daily stats: revenue, order count, avg order value, avg revenue per table
        List<Object[]> dailyRows = orderRepository.getDailyStats(startOfDay, endOfDay);
        Object[] daily = dailyRows.isEmpty()
                ? new Object[]{BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO}
                : dailyRows.get(0);
        BigDecimal todayRevenue = toBigDecimal(daily[0]);
        long todayOrderCount = toLong(daily[1]);
        BigDecimal avgOrderValue = toBigDecimal(daily[2]);
        BigDecimal avgRevenuePerTable = toBigDecimal(daily[3]);

        // Active orders (FIRED, PREPARING, READY)
        long activeOrderCount = orderRepository.countByStatusIn(
                List.of(OrderStatus.FIRED, OrderStatus.PREPARING, OrderStatus.READY));

        // Orders per hour — fill in zeros for 8 AM–11 PM
        List<Object[]> hourlyRaw = orderRepository.getOrdersPerHour(startOfDay, endOfDay, zone.getId());
        Map<Integer, Long> hourlyMap = hourlyRaw.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        List<DashboardStatsResponse.HourlyOrderCount> ordersByHour = new ArrayList<>();
        for (int h = 8; h <= 23; h++) {
            String label = formatHourLabel(h);
            long count = hourlyMap.getOrDefault(h, 0L);
            ordersByHour.add(new DashboardStatsResponse.HourlyOrderCount(h, label, count));
        }

        // Top 5 selling items
        List<Object[]> topRaw = orderItemRepository.getTopSellingItems(startOfDay, endOfDay);
        List<DashboardStatsResponse.TopSellingItem> topItems = topRaw.stream()
                .map(row -> new DashboardStatsResponse.TopSellingItem(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        toBigDecimal(row[2])
                ))
                .collect(Collectors.toList());

        // Recent orders (last 10)
        List<Order> recent = orderRepository.findTop10ByOrderByCreatedAtDesc();
        List<DashboardStatsResponse.RecentOrderSummary> recentOrders = recent.stream()
                .map(o -> new DashboardStatsResponse.RecentOrderSummary(
                        o.getOrderNumber(),
                        o.getOrderType(),
                        o.getStatus(),
                        o.getTotal(),
                        o.getCreatedAt(),
                        o.getTableId()
                ))
                .collect(Collectors.toList());

        return new DashboardStatsResponse(
                todayRevenue,
                todayOrderCount,
                activeOrderCount,
                avgOrderValue,
                avgRevenuePerTable,
                ordersByHour,
                topItems,
                recentOrders
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Float f) return BigDecimal.valueOf(f.doubleValue());
        if (value instanceof Long l) return BigDecimal.valueOf(l);
        if (value instanceof Integer i) return BigDecimal.valueOf(i.longValue());
        return new BigDecimal(value.toString().trim());
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private String formatHourLabel(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }
}
