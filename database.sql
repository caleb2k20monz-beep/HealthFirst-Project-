-- HealthFirst Pharmacy Inventory Management System

CREATE DATABASE IF NOT EXISTS healthfirst_pims
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE healthfirst_pims;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sale_items;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin', 'Cashier') NOT NULL,
    full_name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    supplier_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT
) ENGINE=InnoDB;

CREATE TABLE medicines (
    medicine_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    company VARCHAR(100) NOT NULL,
    medicine_type VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    quantity_in_stock INT NOT NULL DEFAULT 0 CHECK (quantity_in_stock >= 0),
    reorder_level INT NOT NULL DEFAULT 10 CHECK (reorder_level >= 0),
    expiry_date DATE NOT NULL,
    supplier_id INT NOT NULL,
    CONSTRAINT fk_medicine_supplier FOREIGN KEY (supplier_id)
      REFERENCES suppliers(supplier_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_medicine_name (name),
    INDEX idx_medicine_expiry (expiry_date)
) ENGINE=InnoDB;

CREATE TABLE sales (
    sale_id INT PRIMARY KEY AUTO_INCREMENT,
    sale_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    user_id INT NOT NULL,
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id)
      REFERENCES users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_sales_date (sale_date)
) ENGINE=InnoDB;

CREATE TABLE sale_items (
    sale_item_id INT PRIMARY KEY AUTO_INCREMENT,
    sale_id INT NOT NULL,
    medicine_id INT NOT NULL,
    quantity_sold INT NOT NULL CHECK (quantity_sold > 0),
    price_at_sale DECIMAL(10,2) NOT NULL CHECK (price_at_sale >= 0),
    CONSTRAINT fk_item_sale FOREIGN KEY (sale_id)
      REFERENCES sales(sale_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_item_medicine FOREIGN KEY (medicine_id)
      REFERENCES medicines(medicine_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_sale_items_sale (sale_id),
    INDEX idx_sale_items_medicine (medicine_id)
) ENGINE=InnoDB;


INSERT INTO users(username, password, role, full_name) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Admin', 'System Administrator'),
('cashier', 'c246650737293ddc18fc357393db78d1ecc9d1fd1af95469115e4a29f983359a', 'Cashier', 'Demo Cashier');

INSERT INTO suppliers(name, contact_person, phone, email, address) VALUES
('MedSupply SA', 'Thandi Mokoena', '0115550101', 'orders@medsupply.example', 'Johannesburg, Gauteng'),
('Health Wholesale', 'Peter Dlamini', '0115550102', 'sales@healthwholesale.example', 'Sandton, Gauteng'),
('CarePlus Distributors', 'Nomsa Khumalo', '0115550103', 'info@careplus.example', 'Pretoria, Gauteng');

INSERT INTO medicines(name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
('Paracetamol 500mg', 'HealthFirst Generics', 'Tablet', 25.50, 120, 20, DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 1),
('Amoxicillin 250mg', 'CareMed', 'Capsule', 68.00, 8, 15, DATE_ADD(CURDATE(), INTERVAL 20 DAY), 2),
('Cough Syrup 100ml', 'WellCare', 'Syrup', 49.99, 30, 10, DATE_ADD(CURDATE(), INTERVAL 8 MONTH), 3),
('Antiseptic Cream 30g', 'CareMed', 'Cream', 39.95, 5, 10, DATE_ADD(CURDATE(), INTERVAL 12 DAY), 1),
('Vitamin C 1000mg', 'VitaHealth', 'Tablet', 85.00, 50, 12, DATE_ADD(CURDATE(), INTERVAL 14 MONTH), 2);

-- Optional sample transaction so the Sales and Item-Wise reports have data immediately.
INSERT INTO sales(total_amount, user_id) VALUES (51.00, 2);
SET @sample_sale_id = LAST_INSERT_ID();
INSERT INTO sale_items(sale_id, medicine_id, quantity_sold, price_at_sale)
VALUES (@sample_sale_id, 1, 2, 25.50);
UPDATE medicines SET quantity_in_stock = quantity_in_stock - 2 WHERE medicine_id = 1;
