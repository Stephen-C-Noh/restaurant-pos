-- V5__seed_staff.sql
-- Seed staff users for testing PIN authentication

INSERT INTO users (id, username, password_hash, full_name, role, pin_hash, status)
VALUES
  ('a0000001-0000-0000-0000-000000000001', 'admin',    'N/A', 'Admin User',     'ADMIN',   '$2b$04$NiZyAOpSt9sdAJCVJ6ydHexo5xjKuR2bNd253CWdfqPCHhs9q0vym', 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000002', 'manager1', 'N/A', 'Manager One',    'MANAGER', '$2b$04$cynwVFxUfxTRH.kbkZ.F1e5SGjE.FGF3Rcos0SQ8tiNk1714kXgd6', 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000003', 'server1',  'N/A', 'Server One',     'SERVER',  '$2b$04$N9Vh9OWlV0m.P1AtZn0sBuyvwKTOMpXUESkIugMqp6zPzRurQWh16', 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000004', 'server2',  'N/A', 'Server Two',     'SERVER',  '$2b$04$BDBupTRyYWtvu1yMDqiPe.qJRfrUbJDsrnrveajL4EOQM0q/TpOs.', 'ACTIVE'),
  ('a0000001-0000-0000-0000-000000000005', 'kitchen1', 'N/A', 'Kitchen Staff',  'KITCHEN', NULL,                                                             'ACTIVE')
ON CONFLICT (id) DO NOTHING;
