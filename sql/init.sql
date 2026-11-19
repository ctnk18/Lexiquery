-- Databases
CREATE DATABASE IF NOT EXISTS lexiquery_app;
CREATE DATABASE IF NOT EXISTS lexiquery_target;

-- Least-privilege accounts (CHANGE THESE PASSWORDS outside local dev)
CREATE USER IF NOT EXISTS 'lexiquery_app'@'%' IDENTIFIED BY 'app_pass_change_me';
GRANT ALL PRIVILEGES ON lexiquery_app.* TO 'lexiquery_app'@'%';
CREATE USER IF NOT EXISTS 'lexiquery_ro'@'%' IDENTIFIED BY 'ro_pass_change_me';
GRANT SELECT ON lexiquery_target.* TO 'lexiquery_ro'@'%';   -- SELECT only, enforced by the database itself
FLUSH PRIVILEGES;

-- Sample e-commerce data
USE lexiquery_target;
CREATE TABLE customers (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL, email VARCHAR(120), city VARCHAR(60), created_at DATE);
CREATE TABLE products  (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL, category VARCHAR(40), price DECIMAL(10,2));
CREATE TABLE orders    (id INT PRIMARY KEY AUTO_INCREMENT, customer_id INT NOT NULL, order_date DATE, status VARCHAR(20), total DECIMAL(10,2),
                        FOREIGN KEY (customer_id) REFERENCES customers(id));
CREATE TABLE order_items (id INT PRIMARY KEY AUTO_INCREMENT, order_id INT NOT NULL, product_id INT NOT NULL, quantity INT, unit_price DECIMAL(10,2),
                        FOREIGN KEY (order_id) REFERENCES orders(id), FOREIGN KEY (product_id) REFERENCES products(id));

INSERT INTO customers (name,email,city,created_at) VALUES
 ('Aarav Sharma','aarav@example.com','Indore','2023-01-15'),('Priya Verma','priya@example.com','Bhopal','2023-03-02'),
 ('Rohan Mehta','rohan@example.com','Mumbai','2023-06-20'),('Sneha Patel','sneha@example.com','Indore','2024-02-11'),
 ('Karan Singh','karan@example.com','Delhi','2024-05-09');
INSERT INTO products (name,category,price) VALUES
 ('Laptop','Electronics',55000),('Headphones','Electronics',2500),('Desk Chair','Furniture',7800),('Notebook','Stationery',60),('Water Bottle','Lifestyle',450);
INSERT INTO orders (customer_id,order_date,status,total) VALUES
 (1,'2024-01-10','DELIVERED',57500),(1,'2024-03-14','DELIVERED',2500),(2,'2024-02-02','DELIVERED',7800),(3,'2024-04-21','CANCELLED',55000),
 (4,'2024-06-30','DELIVERED',510),(4,'2024-08-12','SHIPPED',2500),(5,'2024-09-05','DELIVERED',55060),(2,'2024-11-18','DELIVERED',450);
INSERT INTO order_items (order_id,product_id,quantity,unit_price) VALUES
 (1,1,1,55000),(1,2,1,2500),(2,2,1,2500),(3,3,1,7800),(4,1,1,55000),(5,4,1,60),(5,5,1,450),(6,2,1,2500),(7,1,1,55000),(7,4,1,60),(8,5,1,450);
