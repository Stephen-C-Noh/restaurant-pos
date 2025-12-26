-- V3__add_kitchen_section.sql
-- Add kitchen section to menu items

ALTER TABLE menu_items
    ADD COLUMN kitchen_section VARCHAR(20) NOT NULL DEFAULT 'GRILL'
        CHECK (kitchen_section IN ('COLD', 'GRILL', 'FRYER', 'SAUTE', 'APPETIZER', 'DESSERT'));

-- Update existing menu items with appropriate sections
-- (You can customize these based on your actual menu items)
UPDATE menu_items SET kitchen_section = 'GRILL' WHERE name LIKE '%Burger%' OR name LIKE '%Steak%';
UPDATE menu_items SET kitchen_section = 'COLD' WHERE name LIKE '%Salad%';
UPDATE menu_items SET kitchen_section = 'FRYER' WHERE name LIKE '%Fries%' OR name LIKE '%Fried%';
UPDATE menu_items SET kitchen_section = 'SAUTE' WHERE name LIKE '%Pasta%';
UPDATE menu_items SET kitchen_section = 'APPETIZER' WHERE name LIKE '%Soup%' OR name LIKE '%Appetizer%';
UPDATE menu_items SET kitchen_section = 'DESSERT' WHERE name LIKE '%Cake%' OR name LIKE '%Ice Cream%';