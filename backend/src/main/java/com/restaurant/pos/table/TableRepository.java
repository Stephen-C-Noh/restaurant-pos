package com.restaurant.pos.table;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TableRepository extends JpaRepository<RestaurantTable, UUID> {
    List<RestaurantTable> findAllByOrderByZoneAscTableNumberAsc();
    List<RestaurantTable> findByStatus(TableStatus status);
    boolean existsByTableNumber(String tableNumber);
}
