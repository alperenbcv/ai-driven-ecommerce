package com.ecommerce.product.config;

import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductListing;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductListingRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductRepository productRepo;
    private final ProductListingRepository listingRepo;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepo.count() == 0) {
            log.info("Demo seed data yükleniyor...");
            Map<String, Category> cats = loadCategories();
            Map<String, Brand> brands = loadBrands();
            loadProducts(cats, brands);
            log.info("Demo seed tamamlandı: {} ürün", productRepo.count());
        } else {
            log.info("Demo ürün verisi zaten mevcut ({} ürün).", productRepo.count());
        }

        loadListings();
        if (listingRepo.count() > 0) {
            log.info("Toplam {} listing mevcut.", listingRepo.count());
        }

        loadImages();
    }

    private Map<String, Category> loadCategories() {
        List<Category> all = new ArrayList<>();

        Category elektronik = cat("Elektronik", null);
        Category giyim      = cat("Giyim & Moda", null);
        Category spor       = cat("Spor & Outdoor", null);
        Category ev         = cat("Ev & Yaşam", null);
        Category kitap      = cat("Kitap & Kırtasiye", null);

        Category laptop     = cat("Laptop & Notebook", elektronik);
        Category tablet     = cat("Tablet", elektronik);
        Category telefon    = cat("Akıllı Telefon", elektronik);
        Category oyun       = cat("Oyun & Konsol", elektronik);
        Category ses        = cat("Ses Sistemleri", elektronik);
        Category akıllıEv   = cat("Akıllı Ev", elektronik);
        Category aksesuar   = cat("Bilgisayar Aksesuarları", elektronik);

        Category erkekG     = cat("Erkek Giyim", giyim);
        Category kadinG     = cat("Kadın Giyim", giyim);
        Category cocukG     = cat("Çocuk Giyim", giyim);

        Category kos        = cat("Koşu & Fitness", spor);
        Category bisiklet   = cat("Bisiklet", spor);

        Category beyazEsya  = cat("Beyaz Eşya", ev);
        Category mutfak     = cat("Mutfak Eşyaları", ev);

        all.addAll(List.of(elektronik, giyim, spor, ev, kitap,
                laptop, tablet, telefon, oyun, ses, akıllıEv, aksesuar,
                erkekG, kadinG, cocukG, kos, bisiklet, beyazEsya, mutfak));

        return categoryRepo.saveAll(all).stream()
                .collect(Collectors.toMap(Category::getName, Function.identity()));
    }

    private Map<String, Brand> loadBrands() {
        List<String> names = List.of(
            "Apple", "Samsung", "Sony", "Asus", "Xiaomi", "Nike", "Adidas",
            "Logitech", "Microsoft", "LG", "Lenovo", "HP", "Corsair", "Razer",
            "Bose", "JBL", "Philips", "Bosch", "Puma", "New Balance"
        );
        return brandRepo.saveAll(
            names.stream().map(n -> Brand.builder().name(n).build()).toList()
        ).stream().collect(Collectors.toMap(Brand::getName, Function.identity()));
    }


    private void loadProducts(Map<String, Category> c, Map<String, Brand> b) {
        List<Product> all = new ArrayList<>();

        all.add(p("MacBook Pro 14\" M3 Pro 18GB/512GB",
            "Apple M3 Pro çip, 18GB birleşik bellek, 512GB SSD, Liquid Retina XDR ekran. " +
            "Video düzenleme, yazılım geliştirme ve 3D render için en güçlü 14 inç laptop.",
            "54999.99", c.get("Laptop & Notebook"), b.get("Apple"), "4.9", 187));

        all.add(p("MacBook Air 15\" M3 8GB/256GB Uzay Grisi",
            "Apple M3 çip, 8GB RAM, fanless tasarım, 18 saate kadar pil ömrü. " +
            "Sessiz ve hafif — günlük kullanım için mükemmel ultrabook.",
            "42999.99", c.get("Laptop & Notebook"), b.get("Apple"), "4.7", 203));

        all.add(p("Lenovo ThinkPad X1 Carbon Gen 12",
            "Intel Core Ultra 7, 32GB LPDDR5, 1TB NVMe SSD, 14\" IPS ekran. " +
            "MIL-STD-810H dayanıklılık sertifikası, Thunderbolt 4, iş dünyasının tercihi.",
            "38999.99", c.get("Laptop & Notebook"), b.get("Lenovo"), "4.6", 94));

        all.add(p("Asus ROG Zephyrus G16 RTX 4070",
            "AMD Ryzen 9 8945HS, RTX 4070 8GB, 32GB DDR5, 16\" WQXGA 240Hz OLED. " +
            "İnce ve hafif gaming laptop. Oyun ve yaratıcı çalışmalarda rakipsiz.",
            "59999.99", c.get("Laptop & Notebook"), b.get("Asus"), "4.8", 76));

        all.add(p("Asus VivoBook 15 OLED AMD Ryzen 5",
            "AMD Ryzen 5 7530U, 8GB DDR4, 512GB SSD, 15.6\" FHD OLED ekran. " +
            "Öğrenci ve uzak çalışanlar için uygun fiyatlı OLED deneyimi.",
            "14999.99", c.get("Laptop & Notebook"), b.get("Asus"), "4.4", 312));

        all.add(p("HP Spectre x360 14\" 2-in-1 OLED",
            "Intel Core Ultra 5, 16GB LPDDR5, 512GB SSD, 14\" 2K OLED dokunmatik ekran. " +
            "360° dönen ekran, OLED güzelliği, HP Sure View gizlilik ekranı.",
            "36999.99", c.get("Laptop & Notebook"), b.get("HP"), "4.5", 58));

        all.add(p("Lenovo IdeaPad Slim 5 AMD Ryzen 7 7735U",
            "AMD Ryzen 7 7735U, 16GB RAM, 512GB SSD, 15.6\" FHD IPS Anti-Glare. " +
            "Günlük işler, ödevler ve streaming için ideal orta sınıf laptop.",
            "12999.99", c.get("Laptop & Notebook"), b.get("Lenovo"), "4.3", 445));

        all.add(p("Microsoft Surface Laptop 6 Cobalt",
            "Intel Core Ultra 5, 16GB RAM, 256GB SSD, 13.5\" PixelSense dokunmatik. " +
            "Windows 11 Pro dahil, sade tasarım, premium yapı kalitesi.",
            "34999.99", c.get("Laptop & Notebook"), b.get("Microsoft"), "4.4", 67));

        all.add(p("Samsung Galaxy Book4 Pro 360 16\"",
            "Intel Core Ultra 7, 16GB, 512GB, 16\" 3K Dynamic AMOLED dokunmatik, S Pen dahil. " +
            "2-in-1 konvertible, Galaxy ekosistemiyle mükemmel entegrasyon.",
            "37999.99", c.get("Laptop & Notebook"), b.get("Samsung"), "4.5", 43));

        all.add(p("Asus ProArt Studiobook 16 OLED RTX 4060",
            "Intel Core i9-13980HX, RTX 4060 8GB, 32GB DDR5, 16\" 4K OLED 120Hz. " +
            "Renk kalibrasyonlu ekran, Pantone Validated. Grafik tasarımcılar için.",
            "64999.99", c.get("Laptop & Notebook"), b.get("Asus"), "4.7", 29));

        all.add(p("HP Pavilion Gaming 15.6\" RTX 3050",
            "Intel Core i5-13500H, RTX 3050 4GB, 16GB DDR4, 512GB SSD, 144Hz IPS. " +
            "Bütçe dostu oyun laptopu. CS2, Valorant ve Minecraft sorunsuz çalışır.",
            "17999.99", c.get("Laptop & Notebook"), b.get("HP"), "4.2", 287));

        all.add(p("Lenovo Legion Slim 5 Gen 9 RTX 4070",
            "AMD Ryzen 7 8845HS, RTX 4070 8GB, 32GB DDR5, 16\" WQXGA 165Hz IPS. " +
            "İnce gövde, güçlü GPU, MUX Switch. Seyahatsever gamerlerin tercihi.",
            "44999.99", c.get("Laptop & Notebook"), b.get("Lenovo"), "4.7", 118));

        all.add(p("Apple iPhone 15 Pro Max 256GB Doğal Titanyum",
            "A17 Pro çip, titanyum çerçeve, 48MP Triple kamera, USB-C, Action Button. " +
            "ProRes 4K 120fps video kaydı. Apple'ın amiral gemisi.",
            "44999.99", c.get("Akıllı Telefon"), b.get("Apple"), "4.8", 324));

        all.add(p("Apple iPhone 15 128GB Pembe",
            "A16 Bionic çip, Dynamic Island, 48MP ana kamera, USB-C, Ceramic Shield. " +
            "Ana stream kullanım için mükemmel performans ve kamera kalitesi.",
            "29999.99", c.get("Akıllı Telefon"), b.get("Apple"), "4.7", 512));

        all.add(p("Samsung Galaxy S24 Ultra 256GB Titanyum Siyah",
            "Snapdragon 8 Gen 3, entegre S Pen, 200MP Quad kamera, 5000mAh, 45W şarj. " +
            "Galaxy AI özellikler, ProVisual Motor. Fotoğrafçılar için nihai Android.",
            "42999.99", c.get("Akıllı Telefon"), b.get("Samsung"), "4.7", 218));

        all.add(p("Samsung Galaxy S24+ 256GB Kobalt Mor",
            "Snapdragon 8 Gen 3, 12GB RAM, 50MP Ana Kamera, 4900mAh batarya. " +
            "Galaxy AI özellikleri dahil. Ultra'nın büyük ve uygun fiyatlı alternatifi.",
            "34999.99", c.get("Akıllı Telefon"), b.get("Samsung"), "4.6", 143));

        all.add(p("Xiaomi 14 Ultra Titanium 512GB",
            "Snapdragon 8 Gen 3, Leica Summilux Quad kamera, 90W şarj, 5300mAh. " +
            "Telefon sektöründe kamera oyununu değiştiren fotoğraf canavarı.",
            "37999.99", c.get("Akıllı Telefon"), b.get("Xiaomi"), "4.6", 87));

        all.add(p("Xiaomi Redmi Note 13 Pro+ 256GB Siyah",
            "MediaTek Dimensity 7200 Ultra, 200MP OIS kamera, 120W HyperCharge, 5000mAh. " +
            "Orta segment'te amiral gemisi özellikleri. Uygun fiyat-performans.",
            "11999.99", c.get("Akıllı Telefon"), b.get("Xiaomi"), "4.5", 634));

        all.add(p("Samsung Galaxy A55 5G 256GB Awesome Lilac",
            "Exynos 1480, 50MP OIS kamera, IP67 su geçirmezlik, 5000mAh, 25W şarj. " +
            "Sağlam yapı, uzun batarya, siyah ve mor renk seçenekleri.",
            "13999.99", c.get("Akıllı Telefon"), b.get("Samsung"), "4.4", 289));

        all.add(p("Xiaomi Redmi 13C 128GB Siyah",
            "MediaTek Helio G85, 50MP AI kamera, 5000mAh batarya. " +
            "Bütçe segmentinde güvenilir performans ve uzun pil ömrü.",
            "4999.99", c.get("Akıllı Telefon"), b.get("Xiaomi"), "4.2", 987));

        all.add(p("Apple iPhone SE (3. Nesil) 128GB Yıldız Işığı",
            "A15 Bionic çip, Touch ID, 12MP kamera, 5G desteği. " +
            "Kompakt tasarımı seven ve iOS isteyen kullanıcılar için en uygun Apple.",
            "13999.99", c.get("Akıllı Telefon"), b.get("Apple"), "4.3", 178));

        all.add(p("Samsung Galaxy Z Fold6 512GB Gümüş",
            "Snapdragon 8 Gen 3, 7.6\" iç ekran, S Pen desteği, 50MP Kamera. " +
            "Katlanabilir telefon teknolojisinin zirvesi. Çalışma ve eğlenceyi bir arada.",
            "59999.99", c.get("Akıllı Telefon"), b.get("Samsung"), "4.5", 34));

        all.add(p("Xiaomi 13T Pro 512GB Siyah",
            "Dimensity 9200+, Leica Kamera Sistemi, 144Hz AMOLED, 144W Şarj. " +
            "Bir saatte sıfırdan %100 şarj. Fotoğraf odaklı güçlü mid-flagship.",
            "19999.99", c.get("Akıllı Telefon"), b.get("Xiaomi"), "4.5", 156));

        all.add(p("Samsung Galaxy A35 5G 128GB Mavi",
            "Exynos 1380, 50MP OIS kamera, AMOLED 120Hz, 5000mAh, IP67. " +
            "Orta bütçeyle premium deneyim. Sağlam yapı, iyi kamera.",
            "9999.99", c.get("Akıllı Telefon"), b.get("Samsung"), "4.3", 421));

        all.add(p("Apple iPad Pro 13\" M4 WiFi 256GB",
            "Apple M4 çip, Ultra Retina XDR OLED, Apple Pencil Pro + Magic Keyboard uyumlu. " +
            "0.5mm'de en ince Apple ürünü. Tasarımcı, çizim ve prodüktivite için.",
            "52999.99", c.get("Tablet"), b.get("Apple"), "4.8", 67));

        all.add(p("Apple iPad Air 11\" M2 WiFi 128GB",
            "Apple M2 çip, 11\" Liquid Retina ekran, Touch ID, Apple Pencil 2. nesil uyumlu. " +
            "Yaratıcı çalışmalar ve öğrenciler için en iyi denge.",
            "25999.99", c.get("Tablet"), b.get("Apple"), "4.7", 142));

        all.add(p("Samsung Galaxy Tab S9 FE+ 5G 128GB",
            "Exynos 1380, 12.4\" TFT LCD, S Pen dahil, IP68, 10090mAh batarya. " +
            "S Pen deneyimi, büyük ekran, uygun fiyat. Öğrenciler için ideal.",
            "14999.99", c.get("Tablet"), b.get("Samsung"), "4.4", 189));

        all.add(p("Xiaomi Pad 6 Pro 256GB",
            "Snapdragon 8+ Gen 1, 11\" IPS 2.8K 144Hz, 8600mAh, 67W şarj. " +
            "Oyun, film ve çizim için güçlü Android tablet deneyimi.",
            "13999.99", c.get("Tablet"), b.get("Xiaomi"), "4.5", 87));

        all.add(p("Apple iPad mini (6. Nesil) WiFi 256GB",
            "Apple A15 Bionic, 8.3\" Liquid Retina, USB-C, Touch ID, 5G hazır. " +
            "Küçük boyutta büyük güç. Tek elle kullanım için en kompakt iPad.",
            "17999.99", c.get("Tablet"), b.get("Apple"), "4.6", 98));

        all.add(p("Samsung Galaxy Tab A9+ WiFi 64GB",
            "Snapdragon 695, 11\" TFT LCD 90Hz, 7040mAh, Quad hoparlör. " +
            "Aile kullanımı, çocuklar için eğitim ve eğlence tableti.",
            "6999.99", c.get("Tablet"), b.get("Samsung"), "4.2", 312));

        all.add(p("Sony PlayStation 5 + DualSense Beyaz",
            "AMD Zen 2 + RDNA 2 GPU, Ultra Hızlı SSD, 4K 120fps, Ray Tracing. " +
            "DualSense haptic feedback ile gerçekçi oyun hissi.",
            "18999.99", c.get("Oyun & Konsol"), b.get("Sony"), "4.9", 478));

        all.add(p("Microsoft Xbox Series X 1TB",
            "Custom AMD Zen 2 + RDNA 2, 1TB NVMe SSD, 4K 120fps, Xbox Game Pass uyumlu. " +
            "Backward compatibility — Xbox One, 360, hatta orijinal Xbox oyunları çalışır.",
            "16999.99", c.get("Oyun & Konsol"), b.get("Microsoft"), "4.8", 234));

        all.add(p("Asus ROG Ally Z1 Extreme Handheld",
            "AMD Ryzen Z1 Extreme, 512GB SSD, 7\" FHD 120Hz dokunmatik, Windows 11. " +
            "Taşınabilir gaming PC. Steam, Xbox Game Pass, Epic Games Library desteği.",
            "22999.99", c.get("Oyun & Konsol"), b.get("Asus"), "4.6", 89));

        all.add(p("Razer Kishi Ultra Type-C Mobil Oyun Kolu",
            "Haptic feedback, Analog tetikler, L3/R3, Type-C bağlantı. " +
            "Xbox Cloud Gaming ve GeForce NOW ile tam oyun keyfi.",
            "2499.99", c.get("Oyun & Konsol"), b.get("Razer"), "4.3", 156));

        all.add(p("Corsair K70 RGB Pro Mekanik Gaming Klavye",
            "Cherry MX Red switch, per-key RGB aydınlatma, USB passthrough, full N-Key Rollover. " +
            "iCUE uyumlu. FPS oyuncularının vazgeçilmezi.",
            "3999.99", c.get("Oyun & Konsol"), b.get("Corsair"), "4.6", 203));

        all.add(p("Razer DeathAdder V3 Pro Kablosuz Gaming Mouse",
            "Focus Pro 30K optik sensör, 90 saat pil, 4000Hz HyperPolling (kablolu), 63g hafif. " +
            "Ergonomik tasarım, esport performansı.",
            "2799.99", c.get("Oyun & Konsol"), b.get("Razer"), "4.7", 178));

        all.add(p("Asus ROG Swift PG27AQDP 27\" OLED 360Hz",
            "27\" QHD OLED, 360Hz, 0.03ms, G-Sync Ultimate + FreeSync, HDR. " +
            "Profesyonel esport oyuncuları için sınır ötesi görüntü kalitesi.",
            "34999.99", c.get("Oyun & Konsol"), b.get("Asus"), "4.8", 45));

        all.add(p("Sony WH-1000XM5 Kablosuz Kulaklık Siyah",
            "Sektörün en iyi ANC teknolojisi, 30 saat pil, Speak-to-Chat, Multipoint bağlantı. " +
            "LDAC codec desteği. Ofis ve seyahat için en iyi kulaklık.",
            "8999.99", c.get("Ses Sistemleri"), b.get("Sony"), "4.8", 567));

        all.add(p("Apple AirPods Pro (2. Nesil) USB-C",
            "H2 çip, Aktif Gürültü Engelleme, Uyarlamalı Ses, Kişiselleştirilmiş Uzamsal Ses. " +
            "Touch kontrol, IP54, 30 saat toplam pil ömrü.",
            "6999.99", c.get("Ses Sistemleri"), b.get("Apple"), "4.7", 423));

        all.add(p("Bose QuietComfort Ultra Kablosuz Kulaklık",
            "Bose Immersive Audio, CustomTune kişiselleştirme, ANC + Aware modu. " +
            "24 saat pil, katlanabilir tasarım. Premium ses deneyimi.",
            "11999.99", c.get("Ses Sistemleri"), b.get("Bose"), "4.8", 189));

        all.add(p("Samsung Galaxy Buds3 Pro Siyah TWS",
            "ANC, 360 Audio, Hi-Fi kalite, IPX7 su geçirmezlik, 12 saat pil. " +
            "Galaxy ekosistemiyle mükemmel entegrasyon. Küçük, hafif, güçlü.",
            "4499.99", c.get("Ses Sistemleri"), b.get("Samsung"), "4.5", 234));

        all.add(p("Logitech G Pro X 2 Lightspeed Gaming Kulaklık",
            "50mm Pro-G karbon sürücüler, Lightspeed kablosuz, 50 saat pil. " +
            "Ultralight 254g. Profesyonel esport oyuncularının tercihi.",
            "5999.99", c.get("Ses Sistemleri"), b.get("Logitech"), "4.7", 123));

        all.add(p("JBL Xtreme 3 Taşınabilir Bluetooth Hoparlör",
            "IP67 su ve toz geçirmezlik, 15 saat pil, Güç Bankası özelliği. " +
            "360° ses, JBL Partboost ile birden fazla hoparlör bağla.",
            "3999.99", c.get("Ses Sistemleri"), b.get("JBL"), "4.5", 312));

        all.add(p("Sony SRS-XG300 Taşınabilir Hoparlör",
            "IP67, Parti Connect (100+ hoparlör), 25 saat pil, X-Balanced Speaker. " +
            "Outdoor partiler için güçlü bas ve yüksek ses.",
            "4499.99", c.get("Ses Sistemleri"), b.get("Sony"), "4.4", 178));

        all.add(p("Bose SoundLink Max Kablosuz Taşınabilir",
            "PartyMode, IP67, USB-C hızlı şarj, 20 saat pil. " +
            "Bose'un Immersive Audio teknolojisi ile tam dolgu ses.",
            "7999.99", c.get("Ses Sistemleri"), b.get("Bose"), "4.6", 89));

        all.add(p("Logitech MX Master 3S Kablosuz Mouse Grafit",
            "8K DPI MagSpeed scroll, sessiz tıklama, ergonomik, USB-C hızlı şarj. " +
            "Üç cihaza Bluetooth + Logi Bolt. Prodüktivite odaklı profesyoneller için.",
            "2199.99", c.get("Bilgisayar Aksesuarları"), b.get("Logitech"), "4.8", 456));

        all.add(p("Logitech MX Keys S Kablosuz Klavye Grafit",
            "Akıllı aydınlatma, Perfect Stroke tuşları, USB-C, Bluetooth ve Logi Bolt. " +
            "Üç cihaz profili, Flow teknolojisi ile cihazlar arası geçiş.",
            "2499.99", c.get("Bilgisayar Aksesuarları"), b.get("Logitech"), "4.7", 234));

        all.add(p("Apple Magic Mouse Uzay Grisi",
            "Multi-Touch yüzey, Lightning şarj, Bluetooth. " +
            "Mac ekosistemiyle mükemmel entegrasyon ve şık tasarım.",
            "1599.99", c.get("Bilgisayar Aksesuarları"), b.get("Apple"), "4.3", 312));

        all.add(p("Samsung 27\" Odyssey G5 QHD 165Hz Curved",
            "27\" 1000R eğimli VA panel, 2560x1440, 165Hz, 1ms GTG, FreeSync Premium. " +
            "Oyun ve içerik tüketimi için güçlü fiyat-performans.",
            "6999.99", c.get("Bilgisayar Aksesuarları"), b.get("Samsung"), "4.5", 187));

        all.add(p("LG 27\" UltraFine 4K USB-C Monitör",
            "27\" IPS 4K, USB-C 96W şarj, Thunderbolt 3, Nano IPS, DCI-P3 %99. " +
            "Mac kullanıcıları ve renk odaklı çalışanlar için referans monitör.",
            "12999.99", c.get("Bilgisayar Aksesuarları"), b.get("LG"), "4.6", 98));

        all.add(p("Corsair HS80 RGB Wireless Gaming Kulaklık",
            "Dolby Atmos 7.1, 60 saat kablosuz pil, Slipstream USB dongle. " +
            "Hafif konstrüksiyon, uzun oturum konforu.",
            "2799.99", c.get("Bilgisayar Aksesuarları"), b.get("Corsair"), "4.5", 156));

        all.add(p("Razer Blade 14\" Araç Çantası",
            "Su geçirmez kapak, laptop bölmesi, aksesuvar cebi. " +
            "Seyahat ve iş kullanımı için dayanıklı ve şık dizüstü çantası.",
            "899.99", c.get("Bilgisayar Aksesuarları"), b.get("Razer"), "4.2", 89));

        all.add(p("Logitech StreamCam Full HD Webcam",
            "1080p 60fps, AI yüz takibi, USB-C, Portrait veya Landscape mod. " +
            "Streamer ve remote worker'lar için profesyonel kamera.",
            "1799.99", c.get("Bilgisayar Aksesuarları"), b.get("Logitech"), "4.4", 203));

        all.add(p("Nike Air Max 270 Erkek Koşu Ayakkabısı Siyah",
            "Max Air yastıklama, mesh üst yüzey, hafif EVA taban. " +
            "Şehir koşusu ve günlük kullanım için konfor odaklı tasarım.",
            "2499.99", c.get("Koşu & Fitness"), b.get("Nike"), "4.5", 312));

        all.add(p("Nike Air Zoom Pegasus 41 Kadın",
            "React köpük orta taban, Zoom Air ön taban, mesh upper. " +
            "Günlük antrenman ve yarı maraton için sektörün en güvenilir koşu ayakkabısı.",
            "2799.99", c.get("Koşu & Fitness"), b.get("Nike"), "4.6", 234));

        all.add(p("Adidas Ultraboost 24 Erkek Beyaz/Gri",
            "Continental™ lastik taban, BOOST köpük, Primeknit+ üst. " +
            "Hem koşu hem günlük kullanım. Yüzde yüz geri dönüştürülmüş malzeme.",
            "3499.99", c.get("Koşu & Fitness"), b.get("Adidas"), "4.7", 189));

        all.add(p("New Balance 990v6 Made in USA Gri",
            "ENCAP ara taban, nubuk + mesh üst, ABD yapımı kalite. " +
            "Trend tarafından değil konfor ve kalite tarafından yönlendirilen klasik.",
            "4499.99", c.get("Koşu & Fitness"), b.get("New Balance"), "4.8", 87));

        all.add(p("Puma Nitro Elite Carbon Erkek",
            "NITRO Elite köpük, karbon fiber plaka, PUMAGRIP taban. " +
            "Maraton ve PB (kişisel rekor) hedefleyenler için yüksek performans ayakkabısı.",
            "3999.99", c.get("Koşu & Fitness"), b.get("Puma"), "4.6", 56));

        all.add(p("Nike Dri-FIT Tişört Erkek Neon Sarı",
            "Dri-FIT teknolojisi, hafif örgü kumaş, geniş kesim. " +
            "Maraton, gym ve outdoor aktiviteler için ter yönetim tişörtü.",
            "499.99", c.get("Koşu & Fitness"), b.get("Nike"), "4.4", 678));

        all.add(p("Adidas Tiro 24 Eşofman Altı Erkek",
            "Aeroready kumaş, elastik bel, cepli. " +
            "Futbol antrenmanı ve günlük konfor için esnek spor eşofmanı.",
            "599.99", c.get("Koşu & Fitness"), b.get("Adidas"), "4.3", 423));

        all.add(p("Samsung Galaxy Watch7 44mm Gümüş",
            "Exynos W1000, 1.3\" Super AMOLED, vücut kompozisyon, enerji skoru. " +
            "Gelişmiş sağlık takibi ve Galaxy ekosistemi entegrasyonu.",
            "8999.99", c.get("Koşu & Fitness"), b.get("Samsung"), "4.5", 234));

        all.add(p("Samsung 85\" Neo QLED 8K QN900D Smart TV",
            "Neo Quantum 8K çip, Mini LED, 8K AI Upscaling, Dolby Atmos. " +
            "Sinema salonunuzu eve taşıyın. Samsung'un en üst segment modeli.",
            "159999.99", c.get("Beyaz Eşya"), b.get("Samsung"), "4.7", 34));

        all.add(p("LG 65\" OLED evo C4 4K Smart TV",
            "OLED evo panel, α9 AI Gen7 çip, 120Hz, Dolby Vision IQ, Game Optimizer. " +
            "Mükemmel siyah derinliği ve renk doğruluğu. Gece film keyfi için en iyisi.",
            "49999.99", c.get("Beyaz Eşya"), b.get("LG"), "4.9", 167));

        all.add(p("Samsung Galaxy Tab S9 Ultra Grafit 256GB WiFi",
            "14.6\" Dynamic AMOLED 2X, S Pen dahil, DeX modu, IP68. " +
            "Laptop'ı tablet ile değiştirmek isteyenler için güçlü ve büyük ekranlı seçenek.",
            "38999.99", c.get("Tablet"), b.get("Samsung"), "4.6", 89));

        all.add(p("Philips 3200 Serisi Tam Otomatik Espresso Makinesi",
            "LatteGo süt sistemi, 5 farklı kahve içeceği, kolay temizlik. " +
            "Sabah rutinini kolaylaştıran akıllı espresso makinesi.",
            "12999.99", c.get("Mutfak Eşyaları"), b.get("Philips"), "4.6", 234));

        all.add(p("Bosch Serie 6 Bulaşık Makinesi 60cm",
            "EcoSilence Motor, Zeolith Drying, WiFi Connect, A+++ enerji sınıfı. " +
            "Sessiz çalışma, mükemmel kurutma, enerji tasarrufu.",
            "18999.99", c.get("Beyaz Eşya"), b.get("Bosch"), "4.7", 123));

        all.add(p("LG PuriCare AeroTower Hava Temizleyici",
            "360° hava akışı, 4 katmanlı filtreleme, PM0.1 algılama, WiFi bağlantı. " +
            "Salon, yatak odası ve çocuk odası için HEPA filtreli hava arıtma.",
            "13999.99", c.get("Beyaz Eşya"), b.get("LG"), "4.5", 89));

        all.add(p("Samsung Bespoke AI Çamaşır Makinesi 12kg",
            "AI Wash, EcoBubble, QuickDrive, Steam, WiFi, A++ enerji. " +
            "Yapay zeka ile otomatik deterjan dozu. En zor lekeleri temizler.",
            "21999.99", c.get("Beyaz Eşya"), b.get("Samsung"), "4.6", 78));

        all.add(p("Philips Airfryer XXL Premium 7.3L",
            "Rapid Air teknolojisi, dijital panel, 7.3 litre kapasite, 8 ön ayar. " +
            "Yağsız kızartma, fırın, ızgara, kek, pizza pişirme.",
            "4999.99", c.get("Mutfak Eşyaları"), b.get("Philips"), "4.6", 567));

        all.add(p("Atomic Habits — James Clear (Türkçe)",
            "Küçük değişikliklerin büyük sonuçlarına dair devrim niteliğinde bir kitap. " +
            "4 adımlı sistem: ipucu, istek, yanıt, ödül. 20M+ dünyada satılan.",
            "199.99", c.get("Kitap & Kırtasiye"), null, "4.9", 2341));

        all.add(p("Temiz Kod — Robert C. Martin (Clean Code)",
            "Yazılım dünyasının başucu kitabı. İyi kod nasıl yazılır, nasıl okunur. " +
            "Junior'dan Senior'a geçişte okunması zorunlu klasik.",
            "349.99", c.get("Kitap & Kırtasiye"), null, "4.8", 1234));

        all.add(p("Yapay Zeka: Bir Rehber — Ethem Alpaydin",
            "Makine öğrenimi ve derin öğrenmeye giriş. Türkçe yazılmış kapsamlı kaynak. " +
            "Algoritma, uygulama ve etik boyutlarıyla AI'ı anlayın.",
            "249.99", c.get("Kitap & Kırtasiye"), null, "4.7", 456));

        all.add(p("Sapiens — Yuval Noah Harari (Türkçe)",
            "İnsanlığın kısa tarihi. Neden biz diğer türleri geride bıraktık? " +
            "Tarih, biyoloji, ekonomi ve felsefenin kesiştiği çığır açan eser.",
            "179.99", c.get("Kitap & Kırtasiye"), null, "4.9", 3456));

        all.add(p("Düşünme Üzerine Hızlı ve Yavaş — Daniel Kahneman",
            "Nobel ödüllü ekonomistin insan psikolojisini anlatan başyapıtı. " +
            "Sistem 1 (hızlı sezgi) ve Sistem 2 (yavaş mantık) nasıl çalışır?",
            "229.99", c.get("Kitap & Kırtasiye"), null, "4.8", 1567));

        productRepo.saveAll(all);
        log.info("{} ürün kaydedildi.", all.size());
    }


    private void loadListings() {
        if (listingRepo.count() > 0) return;

        List<Product> products = productRepo.findAll();
        List<ProductListing> listings = new ArrayList<>();

        for (Product p : products) {
            listings.add(ProductListing.builder()
                    .product(p)
                    .sellerId(2L)
                    .price(p.getPrice())
                    .active(true)
                    .build());

            if (p.getId() != null && p.getId() % 2 == 0) {
                BigDecimal discounted = p.getPrice()
                        .multiply(new BigDecimal("0.95"))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                listings.add(ProductListing.builder()
                        .product(p)
                        .sellerId(3L)
                        .price(discounted)
                        .active(true)
                        .build());
            }
        }

        listingRepo.saveAll(listings);
        log.info("{} listing eklendi.", listings.size());
    }


    private Category cat(String name, Category parent) {
        return categoryRepo.save(Category.builder().name(name).parent(parent).build());
    }

    private Product p(String name, String desc, String price,
                      Category cat, Brand brand, String avgRating, int reviewCount) {
        return Product.builder()
                .name(name)
                .description(desc)
                .price(new BigDecimal(price))
                .category(cat)
                .brand(brand)
                .averageRating(new BigDecimal(avgRating))
                .reviewCount(reviewCount)
                .build();
    }

    @Transactional
    public void loadImages() {
        Map<Long, List<String>> seedMap = buildSeedMap();

        int added = 0;
        for (Map.Entry<Long, List<String>> entry : seedMap.entrySet()) {
            Long productId = entry.getKey();
            List<String> seeds = entry.getValue();
            productRepo.findById(productId).ifPresent(product -> {
                boolean hasSeedImages = product.getImages().stream()
                        .anyMatch(img -> img.getPublicId() != null && img.getPublicId().startsWith("seed-"));
                if (hasSeedImages) return;

                for (int i = 0; i < seeds.size(); i++) {
                    String url = "https://picsum.photos/seed/" + seeds.get(i) + "/800/600";
                    ProductImage img = ProductImage.builder()
                            .url(url)
                            .publicId("seed-" + productId + "-" + (i + 1))
                            .displayOrder(i + 1)
                            .product(product)
                            .build();
                    product.addImage(img);
                }
                productRepo.save(product);
            });
            added++;
        }
        log.info("Görsel seed tamamlandı: {} ürüne görsel eklendi.", added);
    }

    private Map<Long, List<String>> buildSeedMap() {
        Map<Long, List<String>> m = new LinkedHashMap<>();

        m.put( 1L, List.of("macbook-pro-14", "apple-laptop-keyboard", "macbook-desk"));
        m.put( 2L, List.of("macbook-air-m3", "apple-ultrabook", "laptop-white"));
        m.put( 3L, List.of("thinkpad-x1", "lenovo-business-laptop", "office-laptop"));
        m.put( 4L, List.of("asus-rog-gaming", "gaming-laptop-rgb", "rog-zephyrus"));
        m.put( 5L, List.of("asus-vivobook-oled", "student-laptop", "notebook-desk"));
        m.put( 6L, List.of("hp-spectre-x360", "2in1-laptop-touch", "hp-premium"));
        m.put( 7L, List.of("lenovo-ideapad-slim", "budget-laptop-silver", "laptop-side"));
        m.put( 8L, List.of("surface-laptop-cobalt", "microsoft-laptop-blue", "surface-desk"));
        m.put( 9L, List.of("samsung-galaxy-book4", "samsung-laptop-screen", "laptop-amoled"));
        m.put(10L, List.of("proart-studiobook-oled", "creative-laptop-4k", "asus-creator"));
        m.put(11L, List.of("hp-pavilion-gaming", "budget-gaming-laptop", "gtx-laptop"));
        m.put(12L, List.of("legion-slim-5", "lenovo-gaming-thin", "mux-switch-laptop"));

        m.put(13L, List.of("iphone15-pro-max-titanium", "apple-iphone-titanium", "iphone-triple-camera"));
        m.put(14L, List.of("iphone15-pink", "apple-iphone-pink", "iphone-dynamic-island"));
        m.put(15L, List.of("samsung-s24-ultra", "galaxy-s24-spen", "samsung-flagship"));
        m.put(16L, List.of("samsung-s24-plus-violet", "galaxy-s24-purple", "samsung-amoled"));
        m.put(17L, List.of("xiaomi-14-ultra-leica", "xiaomi-leica-camera", "xiaomi-flagship"));
        m.put(18L, List.of("redmi-note13-pro-plus", "xiaomi-midrange", "redmi-200mp"));
        m.put(19L, List.of("samsung-a55-lilac", "galaxy-a55-purple", "samsung-midrange"));
        m.put(20L, List.of("redmi-13c-budget", "xiaomi-budget-phone", "redmi-black"));
        m.put(21L, List.of("iphone-se-3-starlight", "apple-iphonese-compact", "iphone-touchid"));
        m.put(22L, List.of("samsung-zfold6-silver", "galaxy-fold-open", "foldable-phone"));
        m.put(23L, List.of("xiaomi-13t-pro-leica", "xiaomi-144hz-amoled", "dimensity-flagship"));
        m.put(24L, List.of("samsung-a35-blue", "galaxy-a35-back", "samsung-oled-mid"));

        m.put(25L, List.of("ipad-pro-13-m4", "apple-ipad-pro-oled", "ipad-magic-keyboard"));
        m.put(26L, List.of("ipad-air-11-m2", "apple-ipad-air", "ipad-pencil-desk"));
        m.put(27L, List.of("samsung-tab-s9-fe-plus", "galaxy-tab-spen", "samsung-tablet"));
        m.put(28L, List.of("xiaomi-pad6-pro", "xiaomi-tablet-144hz", "pad6-gaming"));
        m.put(29L, List.of("ipad-mini-6", "apple-ipad-mini", "compact-tablet"));
        m.put(30L, List.of("samsung-tab-a9-plus", "samsung-budget-tablet", "family-tablet"));

        m.put(31L, List.of("ps5-dualsense-white", "playstation5-console", "sony-ps5-controller"));
        m.put(32L, List.of("xbox-series-x", "microsoft-xbox-black", "xbox-controller"));
        m.put(33L, List.of("asus-rog-ally-handheld", "windows-handheld-gaming", "rog-ally-screen"));
        m.put(34L, List.of("razer-kishi-ultra", "mobile-gamepad", "phone-controller"));
        m.put(35L, List.of("corsair-k70-rgb-keyboard", "mechanical-keyboard-rgb", "cherry-mx-switches"));
        m.put(36L, List.of("razer-deathadder-v3-pro", "wireless-gaming-mouse", "ergonomic-mouse"));
        m.put(37L, List.of("asus-rog-swift-oled-360hz", "gaming-monitor-oled", "27-monitor-curved"));

        m.put(38L, List.of("sony-wh1000xm5-black", "over-ear-anc-headphones", "sony-headphones-desk"));
        m.put(39L, List.of("airpods-pro-2-usbc", "apple-airpods-pro", "tws-earbuds-case"));
        m.put(40L, List.of("bose-quietcomfort-ultra", "bose-premium-headphones", "anc-headphones-fold"));
        m.put(41L, List.of("samsung-galaxy-buds3-pro", "samsung-tws-black", "galaxy-earbuds-case"));
        m.put(42L, List.of("logitech-gpro-x2-lightspeed", "gaming-wireless-headset", "esport-headset"));
        m.put(43L, List.of("jbl-xtreme3-outdoor", "portable-bluetooth-speaker", "jbl-rugged"));
        m.put(44L, List.of("sony-srs-xg300", "sony-outdoor-speaker", "party-speaker"));
        m.put(45L, List.of("bose-soundlink-max", "bose-portable-premium", "bose-usbc-speaker"));

        m.put(46L, List.of("logitech-mx-master-3s", "productivity-wireless-mouse", "ergonomic-mouse-desk"));
        m.put(47L, List.of("logitech-mx-keys-s", "wireless-backlit-keyboard", "productivity-keyboard"));
        m.put(48L, List.of("apple-magic-mouse-grey", "apple-mouse-desk", "magic-mouse-aluminum"));
        m.put(49L, List.of("samsung-odyssey-g5-curved", "27-curved-gaming-monitor", "qhd-165hz-monitor"));
        m.put(50L, List.of("lg-ultrafine-4k-usbc", "4k-monitor-mac", "lg-nano-ips-monitor"));
        m.put(51L, List.of("corsair-hs80-wireless", "gaming-headset-dolby", "corsair-headset-rgb"));
        m.put(52L, List.of("laptop-bag-razer", "laptop-backpack-waterproof", "travel-laptop-bag"));
        m.put(53L, List.of("logitech-streamcam-hd", "webcam-streamer", "1080p-60fps-webcam"));

        m.put(54L, List.of("nike-air-max-270-black", "nike-running-shoe", "nike-sneaker-side"));
        m.put(55L, List.of("nike-air-zoom-pegasus-41", "nike-womens-running", "running-shoe-pink"));
        m.put(56L, List.of("adidas-ultraboost-24-white", "adidas-boost-shoe", "adidas-primeknit"));
        m.put(57L, List.of("new-balance-990v6-grey", "new-balance-made-usa", "nb-classic-grey"));
        m.put(58L, List.of("puma-nitro-elite-carbon", "carbon-plate-shoe", "puma-marathon"));
        m.put(59L, List.of("nike-dri-fit-shirt-yellow", "running-tshirt-neon", "workout-shirt"));
        m.put(60L, List.of("adidas-tiro-trackpants", "football-training-pants", "aeroready-pants"));
        m.put(61L, List.of("samsung-galaxy-watch7-silver", "smartwatch-fitness", "galaxy-watch-health"));

        m.put(62L, List.of("samsung-neo-qled-8k-tv", "samsung-85inch-tv", "8k-living-room-tv"));
        m.put(63L, List.of("lg-oled-evo-c4-65", "lg-oled-65-tv", "oled-tv-dark-room"));
        m.put(64L, List.of("samsung-galaxy-tab-s9-ultra", "samsung-144k-tablet", "deX-tablet-laptop"));
        m.put(65L, List.of("philips-espresso-3200", "automatic-coffee-machine", "espresso-latte-machine"));
        m.put(66L, List.of("bosch-dishwasher-60cm", "bosch-serie6-dishwasher", "kitchen-dishwasher"));
        m.put(67L, List.of("lg-puricare-aerotower", "air-purifier-modern", "hepa-air-filter"));
        m.put(68L, List.of("samsung-bespoke-washer", "samsung-ai-washing-machine", "bespoke-laundry"));
        m.put(69L, List.of("philips-airfryer-xxl", "airfryer-kitchen", "philips-airfry-cook"));

        m.put(70L, List.of("atomic-habits-book", "self-help-book-desk", "reading-book-coffee"));
        m.put(71L, List.of("clean-code-book", "programming-book", "software-development-book"));
        m.put(72L, List.of("ai-machine-learning-book", "deep-learning-guide", "tech-book-neural"));
        m.put(73L, List.of("sapiens-book-history", "yuval-harari-book", "bestseller-book"));
        m.put(74L, List.of("thinking-fast-and-slow", "kahneman-psychology-book", "bestseller-economics"));

        return m;
    }
}
