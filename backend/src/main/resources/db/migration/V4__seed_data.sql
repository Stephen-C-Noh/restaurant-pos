-- V4__seed_data.sql
-- Seed data for development and testing

-- Insert Menu Category
INSERT INTO menu_categories (id, name, display_order, active, created_at, updated_at)
VALUES
    ('d3a23660-ab70-4059-ab09-baa6a9186ab8', 'Food & Beverages', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Menu Items

-- GRILL Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('bb4d597e-0524-4ac7-ace6-749d8a36d38f', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Classic Burger', 'Beef patty, lettuce, tomato, cheese, pickles', 'GRILL', 'BURGER-001', 12.99, 0.05, 15, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('0adb2df1-35f8-486b-9421-47e2c0b8b13e', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Grilled Ribeye Steak', '12oz USDA Prime ribeye, herb butter', 'GRILL', 'STEAK-001', 32.99, 0.05, 20, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('906553e2-a492-4aca-9ea7-6db624e2325d', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Margherita Pizza', 'Fresh mozzarella, tomato sauce, basil', 'GRILL', 'PIZZA-001', 14.99, 0.05, 20, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- COLD Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('88a06d9d-0201-4bd7-a42c-09bb3f42128f', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Caesar Salad', 'Romaine lettuce, croutons, parmesan, Caesar dressing', 'COLD', 'SALAD-001', 9.99, 0.05, 10, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('6c7a331d-1758-4747-96ae-785912367f4f', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Caprese Salad', 'Fresh mozzarella, tomatoes, basil, balsamic glaze', 'COLD', 'SALAD-002', 11.99, 0.05, 10, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- FRYER Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('040d648d-5120-48a2-94d9-ee6faa4bcf91', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'French Fries', 'Crispy golden fries with sea salt', 'FRYER', 'FRIES-001', 5.99, 0.05, 8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('760fda12-6788-40c1-bd53-6aca80d4b2f4', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Crispy Chicken Wings', 'Buffalo or BBQ sauce, celery, ranch', 'FRYER', 'WINGS-001', 13.99, 0.05, 15, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- SAUTE Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('07df2efd-cc1e-455a-974c-ddfad0817503', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Shrimp Scampi', 'Garlic butter shrimp, white wine, linguine', 'SAUTE', 'PASTA-002', 22.99, 0.05, 18, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('2fb5167e-176c-4aee-80e3-8f0a2aebea22', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Fettuccine Alfredo', 'Creamy parmesan sauce, fresh fettuccine', 'SAUTE', 'PASTA-001', 16.99, 0.05, 15, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- APPETIZER Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('cff6d47c-4c84-4121-a1e6-2064d0c4b0bb', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Spinach Artichoke Dip', 'Creamy dip with tortilla chips', 'APPETIZER', 'APP-001', 10.99, 0.05, 8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('dd5bb807-f350-488c-b2a8-0b9ebeb1f50f', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'French Onion Soup', 'Caramelized onions, beef broth, gruyere cheese', 'APPETIZER', 'SOUP-001', 8.99, 0.05, 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- DESSERT Items
INSERT INTO menu_items (id, category_id, name, description, kitchen_section, sku, base_price, tax_rate, prep_time_minutes, active, created_at, updated_at)
VALUES
    ('6b24550d-c40b-4e82-b495-8666e92aa0b8', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'Chocolate Lava Cake', 'Warm chocolate cake with molten center, vanilla ice cream', 'DESSERT', 'DESSERT-001', 9.99, 0.05, 12, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('16e07a92-2124-49bd-9b07-8812bc8452e5', 'd3a23660-ab70-4059-ab09-baa6a9186ab8', 'New York Cheesecake', 'Classic cheesecake with berry compote', 'DESSERT', 'DESSERT-002', 8.99, 0.05, 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);