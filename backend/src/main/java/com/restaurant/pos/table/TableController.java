package com.restaurant.pos.table;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SERVER','ADMIN','MANAGER')")
    public ResponseEntity<List<TableResponse>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('SERVER','ADMIN','MANAGER')")
    public ResponseEntity<List<TableResponse>> getAvailableTables() {
        return ResponseEntity.ok(tableService.getAvailableTables());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SERVER','ADMIN','MANAGER')")
    public ResponseEntity<TableResponse> getTableById(@PathVariable UUID id) {
        return ResponseEntity.ok(tableService.getTableById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TableResponse> createTable(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.createTable(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SERVER','ADMIN','MANAGER')")
    public ResponseEntity<TableResponse> updateTableStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTableStatusRequest request) {
        return ResponseEntity.ok(tableService.updateTableStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID id) {
        tableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
