-- Tüm servisler için veritabanlarını oluşturur.
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE stock_db;
CREATE DATABASE payment_db;
CREATE DATABASE cargo_db;
CREATE DATABASE search_db;
CREATE DATABASE recommendation_db;

\c product_db;
CREATE EXTENSION IF NOT EXISTS vector;

\c search_db;
CREATE EXTENSION IF NOT EXISTS vector;

\c postgres;
