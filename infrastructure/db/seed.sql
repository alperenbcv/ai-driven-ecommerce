-- ============================================================
-- n12 Demo Seed Data (İSTEĞE BAĞLI — MANUEL ÇALIŞTIRMA)
--
-- UYARI: Bu dosyayı docker-entrypoint-initdb.d altına KOYMAYIN.
-- Tablolar Spring Boot / JPA ile servis ayağa kalkınca oluşur; init aşamasında
-- INSERT'ler "relation does not exist" ile düşer.
--
-- Docker Compose bu dosyayı otomatik çalıştırmaz; demo veri için bkz.:
--   user-service, product-service içindeki DataLoader / CommandLineRunner.
--
-- Bu SQL yalnızca servisler bir kez başlayıp şemayı oluşturduktan sonra
-- elle psql ile yüklemek istenirse kullanılabilir.
-- ============================================================

-- ─── USER_DB ────────────────────────────────────────────────
\c user_db;

-- Önce tabloların oluşmasını beklemek yerine INSERT OR IGNORE kullanıyoruz.
-- Spring Boot başlayınca tablolar oluşur; seed data daha sonra yüklenebilir.
-- Bu yüzden seed data'yı ayrı bir Flyway migration veya CommandLineRunner ile de yükleyebilirsiniz.

-- Demo kullanıcılar (BCrypt hash: "Sifre123!")
INSERT INTO users (first_name, last_name, email, password, role, active, created_at, updated_at)
VALUES
  ('Ali',    'Yılmaz',  'ali@demo.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',   true, NOW(), NOW()),
  ('Satıcı', 'Demo',    'seller@demo.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'SELLER', true, NOW(), NOW()),
  ('Admin',  'Kullanıcı','admin@demo.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN',  true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Demo adresler
INSERT INTO addresses (user_id, title, first_name, last_name, phone, city, district, full_address, default_address, created_at, updated_at)
SELECT u.id, 'Ev', 'Ali', 'Yılmaz', '05321234567', 'İstanbul', 'Kadıköy',
       'Moda Mahallesi, Bahariye Caddesi No:42 D:5', true, NOW(), NOW()
FROM users u WHERE u.email = 'ali@demo.com'
ON CONFLICT DO NOTHING;

-- ─── PRODUCT_DB ─────────────────────────────────────────────
\c product_db;

-- Kategoriler (hiyerarşik)
INSERT INTO categories (name, parent_id, created_at, updated_at) VALUES
  ('Elektronik',        NULL, NOW(), NOW()),
  ('Giyim',             NULL, NOW(), NOW()),
  ('Spor & Outdoor',    NULL, NOW(), NOW()),
  ('Ev & Yaşam',        NULL, NOW(), NOW()),
  ('Kitap & Kırtasiye', NULL, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO categories (name, parent_id, created_at, updated_at)
SELECT 'Bilgisayar & Tablet', id, NOW(), NOW() FROM categories WHERE name = 'Elektronik'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, parent_id, created_at, updated_at)
SELECT 'Telefon & Aksesuar', id, NOW(), NOW() FROM categories WHERE name = 'Elektronik'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, parent_id, created_at, updated_at)
SELECT 'Oyun & Oyun Konsolları', id, NOW(), NOW() FROM categories WHERE name = 'Elektronik'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, parent_id, created_at, updated_at)
SELECT 'Erkek Giyim', id, NOW(), NOW() FROM categories WHERE name = 'Giyim'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, parent_id, created_at, updated_at)
SELECT 'Kadın Giyim', id, NOW(), NOW() FROM categories WHERE name = 'Giyim'
ON CONFLICT DO NOTHING;

-- Markalar
INSERT INTO brands (name, created_at, updated_at) VALUES
  ('Apple',     NOW(), NOW()),
  ('Samsung',   NOW(), NOW()),
  ('Sony',      NOW(), NOW()),
  ('Asus',      NOW(), NOW()),
  ('Xiaomi',    NOW(), NOW()),
  ('Nike',      NOW(), NOW()),
  ('Adidas',    NOW(), NOW()),
  ('Logitech',  NOW(), NOW()),
  ('Microsoft', NOW(), NOW()),
  ('LG',        NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Ürünler (Admin tarafından oluşturulan katalog)
INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'MacBook Pro 14" M3',
  'Apple M3 çip, 18GB RAM, 512GB SSD. ProMotion ekran, uzun pil ömrü. Profesyoneller için tasarlandı.',
  54999.99,
  c.id, b.id, 4.8, 124, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Bilgisayar & Tablet' AND b.name = 'Apple'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'iPhone 15 Pro Max 256GB',
  'A17 Pro çip, titanyum gövde, USB-C, 48MP kamera sistemi. Akıllı telefon pazarının zirvesi.',
  44999.99,
  c.id, b.id, 4.7, 89, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Telefon & Aksesuar' AND b.name = 'Apple'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Samsung Galaxy S24 Ultra',
  'Snapdragon 8 Gen 3, S Pen dahil, 200MP kamera, 5000mAh batarya. Android amiral gemisi.',
  42999.99,
  c.id, b.id, 4.6, 67, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Telefon & Aksesuar' AND b.name = 'Samsung'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Sony PlayStation 5',
  'Yeni nesil oyun konsolu. SSD ile anlık yükleme, DualSense haptic feedback, 4K 120fps gaming.',
  18999.99,
  c.id, b.id, 4.9, 203, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Oyun & Oyun Konsolları' AND b.name = 'Sony'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'ASUS ROG Swift PG27UQ Gaming Monitör',
  '27 inç 4K 144Hz IPS panel, G-Sync Ultimate, HDR1000. Rekabetçi oyunlar için tasarlandı.',
  14999.99,
  c.id, b.id, 4.5, 45, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Bilgisayar & Tablet' AND b.name = 'Asus'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Xiaomi Redmi Note 13 Pro',
  '200MP kamera, 120Hz AMOLED ekran, 5100mAh batarya. Uygun fiyatlı flagship katil.',
  9999.99,
  c.id, b.id, 4.3, 156, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Telefon & Aksesuar' AND b.name = 'Xiaomi'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Nike Air Max 270',
  'Max Air yastıklama teknolojisi, mesh üst yüzey, hafif yapı. Günlük ve spor kullanım için ideal.',
  2499.99,
  c.id, b.id, 4.4, 78, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Spor & Outdoor' AND b.name = 'Nike'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Logitech MX Master 3S',
  'Sessiz tıklama, 8K DPI, ergonomik tasarım, USB-C hızlı şarj. Profesyonel iş akışı için.',
  2199.99,
  c.id, b.id, 4.7, 234, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Bilgisayar & Tablet' AND b.name = 'Logitech'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Samsung 65" Neo QLED 4K TV',
  '65 inç Neo QLED, Mini LED, Quantum HDR 2000, Object Tracking Sound+. Salon deneyimi.',
  34999.99,
  c.id, b.id, 4.6, 92, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Ev & Yaşam' AND b.name = 'Samsung'
ON CONFLICT DO NOTHING;

INSERT INTO products (name, description, price, category_id, brand_id, average_rating, review_count, active, embedding_generated, created_at, updated_at)
SELECT
  'Sony WH-1000XM5 Kablosuz Kulaklık',
  'Sektörün en iyi gürültü engelleme teknolojisi, 30 saat pil, multipoint bağlantı. Premium ses.',
  8999.99,
  c.id, b.id, 4.8, 312, true, false, NOW(), NOW()
FROM categories c, brands b
WHERE c.name = 'Elektronik' AND b.name = 'Sony'
ON CONFLICT DO NOTHING;

-- ─── STOCK_DB ───────────────────────────────────────────────
-- Not: Bu tablolar product_id ile eşleşmeli.
-- Spring Boot başladıktan sonra manuel stock eklemek için:
-- POST /api/stock {productId: X, initialQuantity: 100, lowStockThreshold: 10}
-- veya aşağıdaki SQL'i stock_db'ye çalıştırın:
\c stock_db;

-- Stok verisini manuel olarak ekle (productId'ler product tablosundaki id'lerle eşleşmeli)
-- Bu seed çalıştırıldığında product_db henüz tam oluşmamış olabilir.
-- Gerçek demo için uygulama başladıktan sonra Stock REST API'si kullanın.

-- ─── NEO4J SEED ─────────────────────────────────────────────
-- Neo4j seed'i Cypher ile recommendation-service başladıktan sonra yapılabilir.
-- Örnek: MERGE (u:User {userId: 1}) MERGE (p:Product {productId: 1, name: 'MacBook Pro'}) ...
