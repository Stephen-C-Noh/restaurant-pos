-- V10__kitchen_pin.sql
-- The kitchen display logs in as kitchen1 (PIN 7890, bcrypt cost 4 like V5)

UPDATE users
SET pin_hash = '$2a$04$R3l54uC2TpQ6mFaYUf/zFO3DqTNcfjbBYD2y6l8vDnnu9IthgFVD.'
WHERE id = 'a0000001-0000-0000-0000-000000000005';
