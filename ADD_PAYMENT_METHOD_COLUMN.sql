-- ===== ADD PAYMENT_METHOD COLUMN TO CICILAN_REQUEST TABLE =====
-- Migration: Add payment_method column to cicilan_request table

ALTER TABLE cicilan_request 
ADD COLUMN payment_method VARCHAR(20) DEFAULT 'SIMULATION' AFTER briva;

-- Create index on payment_method for faster queries
CREATE INDEX idx_cicilan_payment_method ON cicilan_request(payment_method);
