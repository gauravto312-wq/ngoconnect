-- V2__reconcile_schema.sql
-- Additive migration for Contact Inquiries, NGO/Restaurant Photos, Two-Stage Handover, Paid Tasks & Food Drives 2.0

-- 1. Contact Inquiries Table
CREATE TABLE IF NOT EXISTS contact_inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contact_inquiries_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Add Profile and Gallery Photos to NGOs
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ngos' AND COLUMN_NAME = 'profile_image_url');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE ngos ADD COLUMN profile_image_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE s1 FROM @stmt; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ngos' AND COLUMN_NAME = 'gallery_photos');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE ngos ADD COLUMN gallery_photos TEXT NULL', 'SELECT 1');
PREPARE s2 FROM @stmt; EXECUTE s2; DEALLOCATE PREPARE s2;

-- 3. Add Profile and Gallery Photos to Restaurants
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'restaurants' AND COLUMN_NAME = 'profile_image_url');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE restaurants ADD COLUMN profile_image_url VARCHAR(500) NULL', 'SELECT 1');
PREPARE s3 FROM @stmt; EXECUTE s3; DEALLOCATE PREPARE s3;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'restaurants' AND COLUMN_NAME = 'gallery_photos');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE restaurants ADD COLUMN gallery_photos TEXT NULL', 'SELECT 1');
PREPARE s4 FROM @stmt; EXECUTE s4; DEALLOCATE PREPARE s4;

-- 4. Add Two-Stage Handover (picked_up_at) to Donations
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'donations' AND COLUMN_NAME = 'picked_up_at');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE donations ADD COLUMN picked_up_at DATETIME NULL', 'SELECT 1');
PREPARE s5 FROM @stmt; EXECUTE s5; DEALLOCATE PREPARE s5;

-- 5. Add Paid Task and Dispute Columns to Volunteer Tasks
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'is_paid');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN is_paid BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE s6 FROM @stmt; EXECUTE s6; DEALLOCATE PREPARE s6;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'payment_amount');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN payment_amount DECIMAL(10,2) NULL', 'SELECT 1');
PREPARE s7 FROM @stmt; EXECUTE s7; DEALLOCATE PREPARE s7;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'currency');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN currency VARCHAR(10) DEFAULT ''INR''', 'SELECT 1');
PREPARE s8 FROM @stmt; EXECUTE s8; DEALLOCATE PREPARE s8;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'payment_terms');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN payment_terms VARCHAR(255) NULL', 'SELECT 1');
PREPARE s9 FROM @stmt; EXECUTE s9; DEALLOCATE PREPARE s9;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'payment_status');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN payment_status VARCHAR(20) DEFAULT ''NOT_APPLICABLE''', 'SELECT 1');
PREPARE s10 FROM @stmt; EXECUTE s10; DEALLOCATE PREPARE s10;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'paid_at');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN paid_at DATETIME NULL', 'SELECT 1');
PREPARE s11 FROM @stmt; EXECUTE s11; DEALLOCATE PREPARE s11;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'paid_by_user_id');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN paid_by_user_id BIGINT NULL', 'SELECT 1');
PREPARE s12 FROM @stmt; EXECUTE s12; DEALLOCATE PREPARE s12;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'disputed_at');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN disputed_at DATETIME NULL', 'SELECT 1');
PREPARE s13 FROM @stmt; EXECUTE s13; DEALLOCATE PREPARE s13;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_tasks' AND COLUMN_NAME = 'dispute_reason');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE volunteer_tasks ADD COLUMN dispute_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE s14 FROM @stmt; EXECUTE s14; DEALLOCATE PREPARE s14;

-- 6. Add Food Drives 2.0 Completion and Photo Submission Columns
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_drives' AND COLUMN_NAME = 'completion_deadline');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE food_drives ADD COLUMN completion_deadline DATETIME NULL', 'SELECT 1');
PREPARE s15 FROM @stmt; EXECUTE s15; DEALLOCATE PREPARE s15;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_drives' AND COLUMN_NAME = 'completed_at');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE food_drives ADD COLUMN completed_at DATETIME NULL', 'SELECT 1');
PREPARE s16 FROM @stmt; EXECUTE s16; DEALLOCATE PREPARE s16;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_drives' AND COLUMN_NAME = 'evidence_photos');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE food_drives ADD COLUMN evidence_photos TEXT NULL', 'SELECT 1');
PREPARE s17 FROM @stmt; EXECUTE s17; DEALLOCATE PREPARE s17;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_drives' AND COLUMN_NAME = 'photos_submitted');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE food_drives ADD COLUMN photos_submitted BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE s18 FROM @stmt; EXECUTE s18; DEALLOCATE PREPARE s18;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'food_drives' AND COLUMN_NAME = 'photos_submitted_at');
SET @stmt = IF(@col_exists = 0, 'ALTER TABLE food_drives ADD COLUMN photos_submitted_at DATETIME NULL', 'SELECT 1');
PREPARE s19 FROM @stmt; EXECUTE s19; DEALLOCATE PREPARE s19;
