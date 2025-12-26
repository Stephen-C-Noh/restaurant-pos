package com.restaurant.pos.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // Menu Items
    @GetMapping("/items")
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        return ResponseEntity.ok(menuService.getAllMenuItems());
    }

    @GetMapping("/items/active")
    public ResponseEntity<List<MenuItem>> getActiveMenuItems() {
        return ResponseEntity.ok(menuService.getActiveMenuItems());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MenuItem> getMenuItem(@PathVariable UUID id) {
        return ResponseEntity.ok(menuService.getMenuItemById(id));
    }

    @PostMapping("/items")
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItem menuItem) {
        MenuItem created = menuService.createMenuItem(menuItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<MenuItem> updateMenuItem(
            @PathVariable UUID id,
            @RequestBody MenuItem menuItem) {
        MenuItem updated = menuService.updateMenuItem(id, menuItem);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable UUID id) {
        menuService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    // Categories
    @GetMapping("/categories")
    public ResponseEntity<List<MenuCategory>> getAllCategories() {
        return ResponseEntity.ok(menuService.getAllCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<MenuCategory> createCategory(@RequestBody MenuCategory category) {
        MenuCategory created = menuService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}