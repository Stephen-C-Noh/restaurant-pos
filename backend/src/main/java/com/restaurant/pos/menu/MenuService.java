package com.restaurant.pos.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    // Menu Items
    @Transactional(readOnly = true)
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getActiveMenuItems() {
        return menuItemRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public MenuItem getMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));
    }

    @Transactional
    public MenuItem createMenuItem(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    @Transactional
    public MenuItem updateMenuItem(UUID id, MenuItem updates) {
        MenuItem existing = getMenuItemById(id);

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getBasePrice() != null) existing.setBasePrice(updates.getBasePrice());
        if (updates.getActive() != null) existing.setActive(updates.getActive());
        if (updates.getCategoryId() != null) existing.setCategoryId(updates.getCategoryId());

        return menuItemRepository.save(existing);
    }

    @Transactional
    public void deleteMenuItem(UUID id) {
        menuItemRepository.deleteById(id);
    }

    // Categories
    @Transactional(readOnly = true)
    public List<MenuCategory> getAllCategories() {
        return menuCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public MenuCategory createCategory(MenuCategory category) {
        return menuCategoryRepository.save(category);
    }
}