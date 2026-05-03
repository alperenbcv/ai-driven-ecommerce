-- Demo başlangıç verilerini yükleyen SQL dosyasıdır.
--
-- Bu script farklı mikroservis veritabanlarına bağlanarak örnek kullanıcı,
-- adres, kategori, marka, ürün ve stok kayıtları oluşturur.
--
-- Amaç:
-- - Uygulama ilk açıldığında test edilebilir hazır veri sağlamak
-- - Login, ürün listeleme, kategori/marka filtreleme, stok kontrolü gibi
--   temel akışları manuel veri girmeden deneyebilmek
-- - Bootcamp/demo ortamında frontend ve backend entegrasyonunu hızlıca göstermek
--
-- ON CONFLICT DO NOTHING kullanıldığı için aynı veri daha önce eklenmişse
-- tekrar çalıştırıldığında duplicate kayıt oluşturmaz.

\c user_db;

INSERT INTO users (first_name, last_name, email, password, role, active, created_at, updated_at)
VALUES
  ('Ali',    'Yılmaz',  'ali@demo.com',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',   true, NOW(), NOW()),
  ('Satıcı', 'Demo',    'seller@demo.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'SELLER', true, NOW(), NOW()),
  ('Admin',  'Kullanıcı','admin@demo.com',  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN',  true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO addresses (user_id, title, first_name, last_name, phone, city, district, full_address, default_address, created_at, updated_at)
SELECT u.id, 'Ev', 'Ali', 'Yılmaz', '05321234567', 'İstanbul', 'Kadıköy',
       'Moda Mahallesi, Bahariye Caddesi No:42 D:5', true, NOW(), NOW()
FROM users u WHERE u.email = 'ali@demo.com'
ON CONFLICT DO NOTHING;

\c product_db;

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

\c stock_db;
