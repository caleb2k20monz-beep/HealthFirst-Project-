# HealthFirst - Pharmacy Inventory Management System (PIMS)

Final Year Assignment - Programming 632 | Diploma in IT - Software Development
Java Swing + MySQL + JDBC Desktop Application

## What it does
HealthFirst replaces manual record-keeping for a local pharmacy.
- Manage stock, expiry dates, suppliers
- Process sales via Point of Sale (POS)
- Generate business reports
- Role-based access control

## Features

### 1. Authentication Module
- Secure login screen
- Role-based redirection: AdminDashboard vs CashierDashboard
- Error handling for failed logins

### 2. Cashier Module
- POS Interface: Add items to cart, checkout
- Billing: Generate bill for customer
- Stock Check: Check price & availability (no edit rights)

### 3. Admin Module
- Medicine Management: Full CRUD
- Supplier Management: Full CRUD
- User Management: Create / Delete Cashier accounts
- Reports: Sales Report, Item-Wise, Low Stock, Expiry Report

## Tech Stack
- Java (Swing / AWT)
- MySQL
- JDBC (PreparedStatement)
- NetBeans / IntelliJ

## Database
5 tables: users, suppliers, medicines, sales, sale_items
See `database.sql` for CREATE + sample data

## How to run
1. Import `database.sql` in MySQL
2. Open project in NetBeans / IntelliJ
3. Update DB credentials in connection class
4. Run Main.java

## Default Logins
- Admin: admin / admin123
- Cashier: cashier / cash123
