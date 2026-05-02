package com.ecommerce.search.config;

import com.ecommerce.search.entity.ProductDocument;
import com.ecommerce.search.repository.ProductDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Search Service Demo Seed Data.
 *
 * Product Service'teki DataLoader doğrudan DB'ye ürün yazar —
 * RabbitMQ event'i yayınlamaz. Bu yüzden search-service'in
 * ürünleri indexlemesi için ayrı bir DataLoader gerekiyor.
 *
 * Strateji:
 * 1. 80 ürünün ProductDocument kaydını oluştur (embeddingGenerated=false)
 * 2. SearchServiceImpl'deki @Scheduled job (ilk 1 dk sonra) bu kayıtları bulur
 * 3. OpenAI embedding API çağrılarak vektörler üretilir → pgvector'e kaydedilir
 * 4. Kullanıcı arama yaptığında semantic search çalışır
 *
 * Bu yaklaşım "eventual consistency" sağlar:
 * Uygulama başladığında arama hemen çalışmaz,
 * embedding üretimi tamamlandığında (~2-3 dk) devreye girer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class SearchDataLoader implements CommandLineRunner {

    private final ProductDocumentRepository documentRepository;

    @Override
    public void run(String... args) {
        if (documentRepository.count() > 0) {
            log.info("Search index zaten dolu ({} belge), atlanıyor.", documentRepository.count());
            return;
        }

        log.info("Search index seed data yükleniyor...");
        List<ProductDocument> docs = createDocuments();
        documentRepository.saveAll(docs);
        log.info("{} ürün arama index'e eklendi. Embedding üretimi 1 dakika içinde başlayacak.", docs.size());
    }

    /**
     * 80 demo ürün için ProductDocument kayıtları.
     * Veriler product-service DataLoader ile eşleşiyor.
     * embeddingGenerated=false → Scheduler 1 dk sonra embedding üretecek.
     */
    private List<ProductDocument> createDocuments() {
        return List.of(
            // ── Laptop & Notebook ──────────────────────────────────────────
            doc(1L,  "MacBook Pro 14\" M3 Pro 18GB/512GB",
                "Apple M3 Pro çip 18GB birleşik bellek 512GB SSD Liquid Retina XDR ekran video düzenleme yazılım geliştirme 3D render",
                "Laptop", "Apple", "54999.99"),
            doc(2L,  "MacBook Air 15\" M3 8GB/256GB Uzay Grisi",
                "Apple M3 çip 8GB fanless 18 saat pil sessiz hafif günlük kullanım ultrabook",
                "Laptop", "Apple", "42999.99"),
            doc(3L,  "Lenovo ThinkPad X1 Carbon Gen 12",
                "Intel Core Ultra 7 32GB 1TB NVMe SSD 14 inç IPS MIL-STD-810H Thunderbolt iş bilgisayarı",
                "Laptop", "Lenovo", "38999.99"),
            doc(4L,  "Asus ROG Zephyrus G16 RTX 4070",
                "AMD Ryzen 9 RTX 4070 32GB DDR5 240Hz OLED gaming oyun laptopu ince hafif",
                "Laptop", "Asus", "59999.99"),
            doc(5L,  "Asus VivoBook 15 OLED AMD Ryzen 5",
                "AMD Ryzen 5 8GB 512GB OLED 15.6 FHD öğrenci uzak çalışma uygun fiyat",
                "Laptop", "Asus", "14999.99"),
            doc(6L,  "HP Spectre x360 14\" 2-in-1 OLED",
                "Intel Core Ultra 5 16GB 512GB OLED dokunmatik 360 derece dönebilen 2in1 katlanabilir laptop",
                "Laptop", "HP", "36999.99"),
            doc(7L,  "Lenovo IdeaPad Slim 5 AMD Ryzen 7 7735U",
                "AMD Ryzen 7 16GB 512GB FHD IPS günlük iş ödev akış streaming orta sınıf",
                "Laptop", "Lenovo", "12999.99"),
            doc(8L,  "Microsoft Surface Laptop 6 Cobalt",
                "Intel Core Ultra 5 16GB 256GB PixelSense dokunmatik Windows 11 Pro sade tasarım premium",
                "Laptop", "Microsoft", "34999.99"),
            doc(9L,  "Samsung Galaxy Book4 Pro 360 16\"",
                "Intel Core Ultra 7 16GB 512GB AMOLED dokunmatik S Pen 2in1 katlanabilir Galaxy ekosistemi",
                "Laptop", "Samsung", "37999.99"),
            doc(10L, "Asus ProArt Studiobook 16 OLED RTX 4060",
                "Intel Core i9 RTX 4060 32GB 4K OLED 120Hz Pantone grafik tasarım fotoğraf video",
                "Laptop", "Asus", "64999.99"),
            doc(11L, "HP Pavilion Gaming 15.6\" RTX 3050",
                "Intel Core i5 RTX 3050 16GB 512GB 144Hz gaming oyun bütçe uygun fiyat CS2 Valorant",
                "Laptop", "HP", "17999.99"),
            doc(12L, "Lenovo Legion Slim 5 Gen 9 RTX 4070",
                "AMD Ryzen 7 RTX 4070 32GB 165Hz MUX Switch ince gaming seyahat gamer",
                "Laptop", "Lenovo", "44999.99"),

            // ── Akıllı Telefon ────────────────────────────────────────────
            doc(13L, "Apple iPhone 15 Pro Max 256GB Doğal Titanyum",
                "A17 Pro titanyum USB-C 48MP triple kamera ProRes 4K 120fps Action Button amiral flagship",
                "Telefon", "Apple", "44999.99"),
            doc(14L, "Apple iPhone 15 128GB Pembe",
                "A16 Bionic Dynamic Island 48MP USB-C Ceramic Shield orta seviye iPhone",
                "Telefon", "Apple", "29999.99"),
            doc(15L, "Samsung Galaxy S24 Ultra 256GB Titanyum Siyah",
                "Snapdragon 8 Gen 3 S Pen 200MP Quad kamera 5000mAh Galaxy AI fotoğraf",
                "Telefon", "Samsung", "42999.99"),
            doc(16L, "Samsung Galaxy S24+ 256GB Kobalt Mor",
                "Snapdragon 8 Gen 3 50MP 4900mAh Galaxy AI büyük ekran",
                "Telefon", "Samsung", "34999.99"),
            doc(17L, "Xiaomi 14 Ultra Titanium 512GB",
                "Snapdragon 8 Gen 3 Leica Summilux kamera 90W şarj 5300mAh fotoğraf",
                "Telefon", "Xiaomi", "37999.99"),
            doc(18L, "Xiaomi Redmi Note 13 Pro+ 256GB Siyah",
                "Dimensity 7200 Ultra 200MP OIS 120W HyperCharge 5000mAh uygun fiyat orta segment",
                "Telefon", "Xiaomi", "11999.99"),
            doc(19L, "Samsung Galaxy A55 5G 256GB Awesome Lilac",
                "Exynos 1480 50MP IP67 su geçirmez 5000mAh 25W şarj sağlam bütçe",
                "Telefon", "Samsung", "13999.99"),
            doc(20L, "Xiaomi Redmi 13C 128GB Siyah",
                "Helio G85 50MP 5000mAh bütçe ekonomik ucuz uygun fiyat",
                "Telefon", "Xiaomi", "4999.99"),
            doc(21L, "Apple iPhone SE (3. Nesil) 128GB Yıldız Işığı",
                "A15 Bionic Touch ID 12MP 5G kompakt küçük iOS Apple ucuz",
                "Telefon", "Apple", "13999.99"),
            doc(22L, "Samsung Galaxy Z Fold6 512GB Gümüş",
                "Snapdragon 8 Gen 3 katlanabilir 7.6 inç S Pen foldable flagship",
                "Telefon", "Samsung", "59999.99"),
            doc(23L, "Xiaomi 13T Pro 512GB Siyah",
                "Dimensity 9200+ Leica 144Hz AMOLED 144W şarj hızlı şarj fotoğraf",
                "Telefon", "Xiaomi", "19999.99"),
            doc(24L, "Samsung Galaxy A35 5G 128GB Mavi",
                "Exynos 1380 50MP AMOLED 120Hz 5000mAh IP67 orta bütçe",
                "Telefon", "Samsung", "9999.99"),

            // ── Tablet ───────────────────────────────────────────────────
            doc(25L, "Apple iPad Pro 13\" M4 WiFi 256GB",
                "M4 çip Ultra Retina XDR OLED Apple Pencil Pro Magic Keyboard tasarım çizim",
                "Tablet", "Apple", "52999.99"),
            doc(26L, "Apple iPad Air 11\" M2 WiFi 128GB",
                "M2 Liquid Retina Touch ID Apple Pencil öğrenci yaratıcı çalışma",
                "Tablet", "Apple", "25999.99"),
            doc(27L, "Samsung Galaxy Tab S9 FE+ 5G 128GB",
                "Exynos 1380 12.4 S Pen dahil IP68 10090mAh büyük ekran öğrenci",
                "Tablet", "Samsung", "14999.99"),
            doc(28L, "Xiaomi Pad 6 Pro 256GB",
                "Snapdragon 8+ Gen 1 11 inç 2.8K 144Hz 8600mAh 67W oyun film çizim",
                "Tablet", "Xiaomi", "13999.99"),
            doc(29L, "Apple iPad mini (6. Nesil) WiFi 256GB",
                "A15 Bionic 8.3 Liquid Retina USB-C kompakt tek elle kullanım taşınabilir",
                "Tablet", "Apple", "17999.99"),
            doc(30L, "Samsung Galaxy Tab A9+ WiFi 64GB",
                "Snapdragon 695 11 inç 90Hz 7040mAh çocuk aile eğitim eğlence",
                "Tablet", "Samsung", "6999.99"),

            // ── Oyun & Konsol ─────────────────────────────────────────────
            doc(31L, "Sony PlayStation 5 + DualSense Beyaz",
                "AMD Zen 2 RDNA 2 SSD 4K 120fps Ray Tracing DualSense haptic feedback oyun konsol",
                "Oyun", "Sony", "18999.99"),
            doc(32L, "Microsoft Xbox Series X 1TB",
                "Custom AMD Zen 2 RDNA 2 1TB SSD 4K 120fps Xbox Game Pass backward compatibility konsol",
                "Oyun", "Microsoft", "16999.99"),
            doc(33L, "Asus ROG Ally Z1 Extreme Handheld",
                "AMD Ryzen Z1 Extreme 512GB 7 inç 120Hz Windows 11 taşınabilir Steam oyun",
                "Oyun", "Asus", "22999.99"),
            doc(34L, "Razer Kishi Ultra Type-C Mobil Oyun Kolu",
                "haptic feedback analog tetik Type-C Xbox Cloud Gaming GeForce NOW mobil",
                "Oyun", "Razer", "2499.99"),
            doc(35L, "Corsair K70 RGB Pro Mekanik Gaming Klavye",
                "Cherry MX Red RGB N-Key Rollover mekanik gaming klavye iCUE FPS",
                "Oyun", "Corsair", "3999.99"),
            doc(36L, "Razer DeathAdder V3 Pro Kablosuz Gaming Mouse",
                "Focus Pro 30K 90 saat pil HyperPolling 63g hafif ergonomik esport",
                "Oyun", "Razer", "2799.99"),
            doc(37L, "Asus ROG Swift PG27AQDP 27\" OLED 360Hz",
                "27 QHD OLED 360Hz 0.03ms G-Sync FreeSync HDR esport gaming monitör",
                "Oyun", "Asus", "34999.99"),

            // ── Ses Sistemleri ────────────────────────────────────────────
            doc(38L, "Sony WH-1000XM5 Kablosuz Kulaklık Siyah",
                "gürültü engelleme ANC 30 saat pil Speak-to-Chat multipoint LDAC kulaklık seyahat ofis",
                "Ses", "Sony", "8999.99"),
            doc(39L, "Apple AirPods Pro (2. Nesil) USB-C",
                "H2 ANC Uyarlamalı Ses Uzamsal Ses IP54 30 saat toplam kulaklık TWS",
                "Ses", "Apple", "6999.99"),
            doc(40L, "Bose QuietComfort Ultra Kablosuz Kulaklık",
                "Bose Immersive Audio CustomTune ANC Aware 24 saat katlanabilir premium kulaklık",
                "Ses", "Bose", "11999.99"),
            doc(41L, "Samsung Galaxy Buds3 Pro Siyah TWS",
                "ANC 360 Audio Hi-Fi IPX7 Galaxy ekosistemi TWS bluetooth kulaklık",
                "Ses", "Samsung", "4499.99"),
            doc(42L, "Logitech G Pro X 2 Lightspeed Gaming Kulaklık",
                "Pro-G karbon sürücüler Lightspeed kablosuz 50 saat pil 254g hafif esport gaming",
                "Ses", "Logitech", "5999.99"),
            doc(43L, "JBL Xtreme 3 Taşınabilir Bluetooth Hoparlör",
                "IP67 su geçirmez 15 saat pil güç bankası 360 ses outdoor dış mekan hoparlör",
                "Ses", "JBL", "3999.99"),
            doc(44L, "Sony SRS-XG300 Taşınabilir Hoparlör",
                "IP67 Parti Connect 25 saat pil X-Balanced parti müzik outdoor güçlü bas",
                "Ses", "Sony", "4499.99"),
            doc(45L, "Bose SoundLink Max Kablosuz Taşınabilir",
                "PartyMode IP67 USB-C 20 saat pil Immersive Audio taşınabilir hoparlör",
                "Ses", "Bose", "7999.99"),

            // ── Bilgisayar Aksesuarları ────────────────────────────────────
            doc(46L, "Logitech MX Master 3S Kablosuz Mouse Grafit",
                "8K DPI MagSpeed sessiz tıklama ergonomik USB-C Bluetooth Logi Bolt prodüktivite mouse",
                "Aksesuar", "Logitech", "2199.99"),
            doc(47L, "Logitech MX Keys S Kablosuz Klavye Grafit",
                "akıllı aydınlatma Perfect Stroke USB-C Bluetooth Flow 3 cihaz klavye prodüktivite",
                "Aksesuar", "Logitech", "2499.99"),
            doc(48L, "Apple Magic Mouse Uzay Grisi",
                "Multi-Touch Lightning Bluetooth Mac ekosistemi şık tasarım mouse",
                "Aksesuar", "Apple", "1599.99"),
            doc(49L, "Samsung 27\" Odyssey G5 QHD 165Hz Curved",
                "27 1000R eğimli VA panel 2560x1440 165Hz 1ms FreeSync gaming monitör",
                "Aksesuar", "Samsung", "6999.99"),
            doc(50L, "LG 27\" UltraFine 4K USB-C Monitör",
                "IPS 4K USB-C 96W şarj Thunderbolt DCI-P3 %99 Mac renk tasarım monitör",
                "Aksesuar", "LG", "12999.99"),
            doc(51L, "Corsair HS80 RGB Wireless Gaming Kulaklık",
                "Dolby Atmos 7.1 60 saat pil Slipstream USB hafif gaming kulaklık",
                "Aksesuar", "Corsair", "2799.99"),
            doc(52L, "Razer Blade 14\" Araç Çantası",
                "su geçirmez laptop çantası aksesuvar cebi seyahat iş",
                "Aksesuar", "Razer", "899.99"),
            doc(53L, "Logitech StreamCam Full HD Webcam",
                "1080p 60fps AI yüz takibi USB-C streamer uzak çalışma kamera webcam",
                "Aksesuar", "Logitech", "1799.99"),

            // ── Spor & Fitness ─────────────────────────────────────────────
            doc(54L, "Nike Air Max 270 Erkek Koşu Ayakkabısı Siyah",
                "Max Air yastıklama mesh şehir koşusu günlük spor ayakkabı erkek",
                "Spor", "Nike", "2499.99"),
            doc(55L, "Nike Air Zoom Pegasus 41 Kadın",
                "React köpük Zoom Air mesh koşu antrenman yarı maraton günlük kadın",
                "Spor", "Nike", "2799.99"),
            doc(56L, "Adidas Ultraboost 24 Erkek Beyaz Gri",
                "BOOST köpük Continental lastik Primeknit koşu günlük geri dönüştürülmüş spor",
                "Spor", "Adidas", "3499.99"),
            doc(57L, "New Balance 990v6 Made in USA Gri",
                "ENCAP nubuk mesh ABD yapımı konfor kalite klasik erkek ayakkabı",
                "Spor", "New Balance", "4499.99"),
            doc(58L, "Puma Nitro Elite Carbon Erkek",
                "NITRO Elite köpük karbon plaka PUMAGRIP maraton PB kişisel rekor koşu",
                "Spor", "Puma", "3999.99"),
            doc(59L, "Nike Dri-FIT Tişört Erkek Neon Sarı",
                "Dri-FIT nem yönetimi hafif örgü koşu gym outdoor antrenman tişört erkek",
                "Spor", "Nike", "499.99"),
            doc(60L, "Adidas Tiro 24 Eşofman Altı Erkek",
                "Aeroready elastik bel cepli futbol antrenman konfor spor eşofman erkek",
                "Spor", "Adidas", "599.99"),
            doc(61L, "Samsung Galaxy Watch7 44mm Gümüş",
                "Exynos W1000 Super AMOLED vücut kompozisyon enerji skoru sağlık koşu akıllı saat",
                "Spor", "Samsung", "8999.99"),

            // ── Ev & Yaşam ────────────────────────────────────────────────
            doc(62L, "Samsung 85\" Neo QLED 8K QN900D Smart TV",
                "Neo Quantum 8K Mini LED AI Upscaling Dolby Atmos büyük ekran TV salon sinema",
                "Ev", "Samsung", "159999.99"),
            doc(63L, "LG 65\" OLED evo C4 4K Smart TV",
                "OLED evo α9 AI 120Hz Dolby Vision Game Optimizer TV salon film gece",
                "Ev", "LG", "49999.99"),
            doc(64L, "Samsung Galaxy Tab S9 Ultra Grafit 256GB WiFi",
                "14.6 Dynamic AMOLED 2X S Pen DeX modu IP68 büyük tablet laptop alternatif",
                "Tablet", "Samsung", "38999.99"),
            doc(65L, "Philips 3200 Serisi Tam Otomatik Espresso Makinesi",
                "LatteGo süt sistemi espresso cappuccino latte kahve makinesi otomatik",
                "Ev", "Philips", "12999.99"),
            doc(66L, "Bosch Serie 6 Bulaşık Makinesi 60cm",
                "EcoSilence Motor Zeolith kurutma WiFi A+++ sessiz bulaşık makinesi ev",
                "Ev", "Bosch", "18999.99"),
            doc(67L, "LG PuriCare AeroTower Hava Temizleyici",
                "360 hava akışı HEPA filtre PM0.1 WiFi salon yatak odası hava temizleyici",
                "Ev", "LG", "13999.99"),
            doc(68L, "Samsung Bespoke AI Çamaşır Makinesi 12kg",
                "AI Wash EcoBubble QuickDrive Steam WiFi A++ çamaşır makinesi ev",
                "Ev", "Samsung", "21999.99"),
            doc(69L, "Philips Airfryer XXL Premium 7.3L",
                "Rapid Air yağsız kızartma 7.3 litre fırın ızgara kek pizza pişirme",
                "Ev", "Philips", "4999.99"),

            // ── Kitap & Kırtasiye ─────────────────────────────────────────
            doc(70L, "Atomic Habits - James Clear Türkçe",
                "alışkanlık kişisel gelişim motivasyon küçük değişiklik büyük sonuç sistem ipucu istek yanıt ödül",
                "Kitap", null, "199.99"),
            doc(71L, "Temiz Kod - Robert C. Martin Clean Code",
                "yazılım mühendisliği kod kalitesi refactoring best practices programlama temiz kod",
                "Kitap", null, "349.99"),
            doc(72L, "Yapay Zeka Rehber - Ethem Alpaydin",
                "makine öğrenimi derin öğrenme algoritma uygulama yapay zeka AI Türkçe",
                "Kitap", null, "249.99"),
            doc(73L, "Sapiens - Yuval Noah Harari Türkçe",
                "insanlık tarihi evrim tarih biyoloji ekonomi felsefe homo sapiens",
                "Kitap", null, "179.99"),
            doc(74L, "Düşünme Üzerine Hızlı ve Yavaş - Daniel Kahneman",
                "psikoloji karar verme düşünce Sistem 1 Sistem 2 Nobel bilişsel davranış",
                "Kitap", null, "229.99")
        );
    }

    private ProductDocument doc(Long id, String name, String desc,
                                 String category, String brand, String price) {
        return ProductDocument.builder()
                .productId(id)
                .name(name)
                .description(desc)
                .category(category)
                .brand(brand)
                .minPrice(new BigDecimal(price))
                .active(true)
                .embeddingGenerated(false)
                .build();
    }
}
