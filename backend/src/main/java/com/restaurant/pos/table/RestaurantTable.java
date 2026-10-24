package com.restaurant.pos.table;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tables")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "table_number", nullable = false, unique = true, length = 10)
    private String tableNumber;

    @Column(nullable = false)
    private int capacity;

    @Column(length = 50)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(name = "current_server_id")
    private UUID currentServerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public TableStatus getStatus() { return status; }
    public void setStatus(TableStatus status) { this.status = status; }

    public UUID getCurrentServerId() { return currentServerId; }
    public void setCurrentServerId(UUID currentServerId) { this.currentServerId = currentServerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
