package com.restaurant.pos.table;

import com.restaurant.pos.common.BusinessRuleException;
import com.restaurant.pos.common.ResourceNotFoundException;
import com.restaurant.pos.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class TableService {

    private static final Map<TableStatus, Set<TableStatus>> VALID_TRANSITIONS = Map.of(
            TableStatus.AVAILABLE, Set.of(TableStatus.OCCUPIED, TableStatus.RESERVED),
            TableStatus.OCCUPIED, Set.of(TableStatus.CLEANING, TableStatus.AVAILABLE),
            TableStatus.RESERVED, Set.of(TableStatus.AVAILABLE, TableStatus.OCCUPIED),
            TableStatus.CLEANING, Set.of(TableStatus.AVAILABLE)
    );

    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    public TableService(TableRepository tableRepository, UserRepository userRepository) {
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        return tableRepository.findAllByOrderByZoneAscTableNumberAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TableResponse> getAvailableTables() {
        return tableRepository.findByStatus(TableStatus.AVAILABLE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TableResponse getTableById(UUID id) {
        return toResponse(findTable(id));
    }

    public TableResponse createTable(CreateTableRequest request) {
        if (tableRepository.existsByTableNumber(request.tableNumber())) {
            throw new BusinessRuleException("Table number already exists: " + request.tableNumber());
        }

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(request.tableNumber());
        table.setCapacity(request.capacity());
        table.setZone(request.zone());

        return toResponse(tableRepository.save(table));
    }

    public TableResponse updateTableStatus(UUID id, UpdateTableStatusRequest request) {
        RestaurantTable table = findTable(id);
        validateStatusTransition(table.getStatus(), request.status());

        table.setStatus(request.status());

        if (request.status() == TableStatus.AVAILABLE || request.status() == TableStatus.CLEANING) {
            table.setCurrentServerId(null);
        } else {
            table.setCurrentServerId(request.serverId());
        }

        return toResponse(tableRepository.save(table));
    }

    public void deleteTable(UUID id) {
        RestaurantTable table = findTable(id);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessRuleException("Cannot delete an occupied table");
        }
        tableRepository.delete(table);
    }

    private RestaurantTable findTable(UUID id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found: " + id));
    }

    private void validateStatusTransition(TableStatus current, TableStatus next) {
        Set<TableStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new BusinessRuleException("Invalid status transition: " + current + " → " + next);
        }
    }

    private TableResponse toResponse(RestaurantTable table) {
        String serverName = null;
        if (table.getCurrentServerId() != null) {
            serverName = userRepository.findById(table.getCurrentServerId())
                    .map(user -> user.getFullName())
                    .orElse(null);
        }

        return new TableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getZone(),
                table.getStatus(),
                table.getCurrentServerId(),
                serverName,
                table.getUpdatedAt()
        );
    }
}
