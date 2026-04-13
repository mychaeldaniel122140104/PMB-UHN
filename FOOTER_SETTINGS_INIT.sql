-- Initialize Footer Settings for PMB System
-- Run these commands in H2 Console at http://localhost:8080/h2-console

-- 1. DELETE old footer settings if exist
DELETE FROM system_configurations WHERE config_key IN (
    'email_support', 'whatsapp_admin', 'bank_nama', 'bank_rekening', 
    'penerima_rekening', 'facebook_url', 'instagram_handle', 'website_url'
);

-- 2. INSERT Footer Settings

-- Email Support
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('email_support', 'pmb@hkbpnommensen.ac.id', 'Email untuk kontak PMB', 'STRING', true, NOW(), NOW());

-- WhatsApp Admin
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('whatsapp_admin', '628123456789', 'Nomor WhatsApp admin PMB (format: 62xxxxx)', 'STRING', true, NOW(), NOW());

-- Bank Name
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('bank_nama', 'BRI (Bank Rakyat Indonesia)', 'Nama Bank untuk rekening PMB', 'STRING', true, NOW(), NOW());

-- Bank Account Number
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('bank_rekening', '1234567890', 'Nomor Rekening untuk pembayaran PMB', 'STRING', true, NOW(), NOW());

-- Account Owner Name
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('penerima_rekening', 'HKBP Nommensen University', 'Nama Penerima Rekening', 'STRING', true, NOW(), NOW());

-- Facebook URL
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('facebook_url', 'https://www.facebook.com/hkbpnommensen', 'URL Facebook HKBP Nommensen', 'STRING', true, NOW(), NOW());

-- Instagram Handle
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('instagram_handle', 'hkbp_nommensen', 'Instagram Handle (tanpa @)', 'STRING', true, NOW(), NOW());

-- Website URL
INSERT INTO system_configurations (config_key, config_value, description, config_type, is_active, created_at, updated_at)
VALUES ('website_url', 'https://www.hkbpnommensen.ac.id', 'Website resmi HKBP Nommensen', 'STRING', true, NOW(), NOW());

-- 3. VERIFY (optional - check if inserted correctly)
-- SELECT * FROM system_configurations WHERE config_key IN ('email_support', 'whatsapp_admin', 'bank_nama', 'bank_rekening', 'penerima_rekening', 'facebook_url', 'instagram_handle', 'website_url');

COMMIT;
