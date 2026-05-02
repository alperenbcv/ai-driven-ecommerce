package com.ecommerce.recommendation.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Neo4j Graf Seed Data — Cypher ile doğrudan yükleme.
 *
 * Spring Data Neo4j repository yerine raw Cypher kullanıyoruz çünkü:
 * - Repository ile erken bağlam başlatımında bazı transactional edge-case
 *   sorunları yaşanmıştı; Driver doğrudan ve öngörülebilirdir.
 * - Neo4j Driver API her zaman düzgün çalışır, herhangi bir bean
 *   initialization sırasına bağımlı değil.
 *
 * MERGE → aynı node varsa güncelle, yoksa oluştur (idempotent).
 * Bu sayede servis her restart'ta güvenle çalışır.
 *
 * Graf yapısı:
 *   (User)-[:PURCHASED]->(Product)  → Satın alma
 *   (User)-[:VIEWED]->(Product)     → Görüntüleme
 *
 * Collaborative Filtering sorgusu (RecommendationServiceImpl'de):
 *   MATCH (u:User {userId:X})-[:PURCHASED]->(p)<-[:PURCHASED]-(other)-[:PURCHASED]->(rec)
 *   WHERE NOT (u)-[:PURCHASED]->(rec)
 *   RETURN rec.productId, count(other) ORDER BY count DESC
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class GraphDataLoader {

    private final Driver driver;

    /**
     * Tek seferlik seed — uygulama tamamen ayağa kalktıktan sonra çalışır ({@link ApplicationReadyEvent}).
     * MERGE sayesinde tekrar güvenle çağrılabilir.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedGraphOnReady() {
        try (Session session = driver.session()) {
            long userCount = session.run("MATCH (u:User) RETURN count(u) AS cnt")
                    .single().get("cnt").asLong();
            if (userCount > 0) {
                log.info("Neo4j grafı zaten dolu ({} kullanıcı), atlanıyor.", userCount);
                return;
            }
        } catch (Exception e) {
            log.warn("Neo4j sayım hatası: {}", e.getMessage());
            return;
        }

        log.info("Neo4j graf seed başlıyor...");
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                // ── Ürün node'ları ──────────────────────────────────────────
                // productId değerleri PostgreSQL catalog.id ile aynı tutulmalı (örnek/demo veri uyumu).
                String[][] products = {
                    {"1",  "MacBook Pro 14\" M3 Pro",          "Laptop"},
                    {"2",  "MacBook Air 15\" M3",              "Laptop"},
                    {"3",  "Lenovo ThinkPad X1 Carbon",        "Laptop"},
                    {"4",  "Asus ROG Zephyrus G16",            "Laptop"},
                    {"5",  "Asus VivoBook 15 OLED",            "Laptop"},
                    {"6",  "HP Spectre x360 14\"",             "Laptop"},
                    {"7",  "Lenovo IdeaPad Slim 5",            "Laptop"},
                    {"8",  "Microsoft Surface Laptop 6",       "Laptop"},
                    {"9",  "Samsung Galaxy Book4 Pro 360",     "Laptop"},
                    {"10", "Asus ProArt Studiobook 16",        "Laptop"},
                    {"11", "HP Pavilion Gaming 15.6\"",        "Laptop"},
                    {"12", "Lenovo Legion Slim 5 Gen 9",       "Laptop"},
                    {"13", "iPhone 15 Pro Max",                "Telefon"},
                    {"14", "iPhone 15",                        "Telefon"},
                    {"15", "Samsung Galaxy S24 Ultra",         "Telefon"},
                    {"16", "Samsung Galaxy S24+",              "Telefon"},
                    {"17", "Xiaomi 14 Ultra",                  "Telefon"},
                    {"18", "Xiaomi Redmi Note 13 Pro+",        "Telefon"},
                    {"19", "Samsung Galaxy A55 5G",            "Telefon"},
                    {"20", "Xiaomi Redmi 13C",                 "Telefon"},
                    {"21", "iPhone SE (3. Nesil)",             "Telefon"},
                    {"22", "Samsung Galaxy Z Fold6",           "Telefon"},
                    {"23", "Xiaomi 13T Pro",                   "Telefon"},
                    {"24", "Samsung Galaxy A35 5G",            "Telefon"},
                    {"25", "iPad Pro 13\" M4",                 "Tablet"},
                    {"26", "iPad Air 11\" M2",                 "Tablet"},
                    {"27", "Samsung Galaxy Tab S9 FE+",        "Tablet"},
                    {"28", "Xiaomi Pad 6 Pro",                 "Tablet"},
                    {"29", "iPad mini 6",                      "Tablet"},
                    {"30", "Samsung Galaxy Tab A9+",           "Tablet"},
                    {"31", "Sony PlayStation 5",               "Oyun"},
                    {"32", "Xbox Series X",                    "Oyun"},
                    {"33", "Asus ROG Ally",                    "Oyun"},
                    {"34", "Razer Kishi Ultra",                "Oyun"},
                    {"35", "Corsair K70 RGB Pro",              "Oyun"},
                    {"36", "Razer DeathAdder V3 Pro",          "Oyun"},
                    {"37", "Asus ROG Swift PG27AQDP",          "Oyun"},
                    {"38", "Sony WH-1000XM5",                  "Ses"},
                    {"39", "AirPods Pro 2. Nesil",             "Ses"},
                    {"40", "Bose QuietComfort Ultra",          "Ses"},
                    {"41", "Samsung Galaxy Buds3 Pro",         "Ses"},
                    {"42", "Logitech G Pro X 2",              "Ses"},
                    {"43", "JBL Xtreme 3",                    "Ses"},
                    {"44", "Sony SRS-XG300",                  "Ses"},
                    {"45", "Bose SoundLink Max",              "Ses"},
                    {"46", "Logitech MX Master 3S",           "Aksesuar"},
                    {"47", "Logitech MX Keys S",              "Aksesuar"},
                    {"48", "Apple Magic Mouse",                "Aksesuar"},
                    {"49", "Samsung Odyssey G5",              "Aksesuar"},
                    {"50", "LG UltraFine 4K Monitör",         "Aksesuar"},
                    {"51", "Corsair HS80 RGB Wireless",       "Aksesuar"},
                    {"52", "Razer Blade Çanta",               "Aksesuar"},
                    {"53", "Logitech StreamCam",              "Aksesuar"},
                    {"54", "Nike Air Max 270",                "Spor"},
                    {"55", "Nike Air Zoom Pegasus 41",        "Spor"},
                    {"56", "Adidas Ultraboost 24",            "Spor"},
                    {"57", "New Balance 990v6",               "Spor"},
                    {"58", "Puma Nitro Elite Carbon",         "Spor"},
                    {"59", "Nike Dri-FIT Tişört",             "Spor"},
                    {"60", "Adidas Tiro 24 Eşofman",          "Spor"},
                    {"61", "Samsung Galaxy Watch7",           "Spor"},
                    {"62", "Samsung 85\" Neo QLED 8K",        "Ev"},
                    {"63", "LG 65\" OLED evo C4",             "Ev"},
                    {"64", "Samsung Galaxy Tab S9 Ultra",     "Tablet"},
                    {"65", "Philips Espresso Makinesi",       "Ev"},
                    {"66", "Bosch Bulaşık Makinesi",          "Ev"},
                    {"67", "LG PuriCare AeroTower",           "Ev"},
                    {"68", "Samsung Bespoke Çamaşır",         "Ev"},
                    {"69", "Philips Airfryer XXL",            "Ev"},
                    {"70", "Atomic Habits",                   "Kitap"},
                    {"71", "Temiz Kod",                       "Kitap"},
                    {"72", "Yapay Zeka Rehber",               "Kitap"},
                    {"73", "Sapiens",                         "Kitap"},
                    {"74", "Düşünme Üzerine",                 "Kitap"},
                };

                for (String[] p : products) {
                    tx.run("MERGE (p:Product {productId: $id}) SET p.name = $name, p.category = $cat",
                            org.neo4j.driver.Values.parameters("id", Long.parseLong(p[0]), "name", p[1], "cat", p[2]));
                }

                // ── Kullanıcı profilleri & ilişkiler ──────────────────────
                seedUser(tx, 1L,  "Ali Yılmaz",
                        new long[]{1,13,39,46,47},        new long[]{2,14,25,38,48});
                seedUser(tx, 2L,  "Satıcı Demo",
                        new long[]{15,16,27,41,49,62},    new long[]{9,30,61,63,66});
                seedUser(tx, 3L,  "Admin Kullanıcı",
                        new long[]{4,31,32,35,36,37,51},  new long[]{12,33,34,42,50});
                seedUser(tx, 10L, "Zeynep Kaya",
                        new long[]{1,13,39,46,47,70,71,73}, new long[]{2,29,38,48});
                seedUser(tx, 11L, "Mert Demir",
                        new long[]{5,20,28,43,73,74},     new long[]{7,18,27,44,72});
                seedUser(tx, 12L, "Selin Arslan",
                        new long[]{1,10,13,25,40,50,71},  new long[]{6,17,26,38,47});
                seedUser(tx, 13L, "Can Öztürk",
                        new long[]{4,12,31,35,36,37,42,46,53}, new long[]{32,33,34,51});
                seedUser(tx, 14L, "Elif Şahin",
                        new long[]{54,55,56,59,60,61,14,39}, new long[]{57,58,18,41,27});
                seedUser(tx, 15L, "Hasan Çelik",
                        new long[]{62,63,65,66,67,68,69,19,24}, new long[]{30,43,44,61,73});
                seedUser(tx, 16L, "Ayşe Doğan",
                        new long[]{3,6,15,22,38,40,49,50,63}, new long[]{1,13,25,32,37});

                return null;
            });

            long uCount = session.run("MATCH (u:User) RETURN count(u) AS cnt").single().get("cnt").asLong();
            long pCount = session.run("MATCH (p:Product) RETURN count(p) AS cnt").single().get("cnt").asLong();
            log.info("Neo4j graf seed tamamlandı: {} kullanıcı, {} ürün node'u.", uCount, pCount);

        } catch (Exception e) {
            log.error("Neo4j graf seed başarısız: {}", e.getMessage(), e);
        }
    }

    private void seedUser(TransactionContext tx, long userId, String name,
                          long[] purchased, long[] viewed) {
        tx.run("MERGE (u:User {userId: $id}) SET u.name = $name",
                org.neo4j.driver.Values.parameters("id", userId, "name", name));
        for (long pid : purchased) {
            tx.run("""
                MATCH (u:User {userId: $uid}), (p:Product {productId: $pid})
                MERGE (u)-[:PURCHASED]->(p)
                """, org.neo4j.driver.Values.parameters("uid", userId, "pid", pid));
        }
        for (long pid : viewed) {
            tx.run("""
                MATCH (u:User {userId: $uid}), (p:Product {productId: $pid})
                MERGE (u)-[:VIEWED]->(p)
                """, org.neo4j.driver.Values.parameters("uid", userId, "pid", pid));
        }
    }
}
