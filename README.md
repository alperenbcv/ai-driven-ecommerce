# Uçtan Uca Mimari ve Akış Dokümanı

## 1. Projenin Amacı

Bu projeyi, klasik bir monolit e-ticaret uygulaması yerine mikroservis mimarisine yakın bir yapı kurarak geliştirdim. Amaç yalnızca ürün listeleme ve sipariş oluşturma gibi temel e-ticaret fonksiyonlarını yapmak değil; aynı zamanda kullanıcı yönetimi, satıcı yönetimi, ürün moderasyonu, stok rezervasyonu, ödeme, kargo, bildirim ve AI destekli asistan özelliklerini birbirinden ayrılmış servisler üzerinden çalıştırmaktı.

Proje production ortamına çıkarılmak üzere tasarlanmadı; bootcamp teslimi kapsamında, sınırlı sürede çalışan, anlaşılır, modüler ve teknik olarak zengin bir demo proje hedeflendi. Bu nedenle bazı noktalarda production seviyesinde çok daha ileri götürülebilecek güvenlik, gözlemlenebilirlik, idempotency, outbox pattern gibi konular bilinçli olarak sade tutuldu. Ancak genel yapı, gerçek bir mikroservis e-ticaret sisteminin temel parçalarını gösterecek şekilde kurgulandı.

Kod içinde sınıf ve metot seviyesindeki teknik detayları ayrıca doc comment'lar ile açıkladığım için bu dokümanda daha çok projenin genel mimarisi, servislerin sorumlulukları, uçtan uca iş akışları ve tamamlanan gereksinimler anlatılmıştır.

---

## 2. Genel Mimari

Projede frontend ve backend ayrık şekilde tasarlandı. Backend tarafında servisler domain sorumluluklarına göre bölündü. Her servis kendi iş alanına odaklanıyor ve diğer servislerle gerektiğinde HTTP, Feign Client veya RabbitMQ eventleri üzerinden haberleşiyor.

Genel servis yapısı şu şekilde:

- **Gateway Service**
- **Discovery Service**
- **User Service**
- **Product Service**
- **Cart Service**
- **Stock Service**
- **Order Service**
- **Payment Service**
- **Cargo Service**
- **Notification Service**
- **Assistant Service**
- **Search / Recommendation tarafı**

Frontend tarafında React + TypeScript tabanlı bir müşteri, satıcı ve admin arayüzü geliştirildi. Kullanıcı; ürünleri gezebiliyor, AI asistanla konuşabiliyor, sepete ürün ekleyebiliyor, ödeme akışını başlatabiliyor ve sipariş/kargo durumunu takip edebiliyor. Satıcı; ürün önerisi gönderebiliyor, onaylanan ürünlere listing açabiliyor, fiyat ve stok yönetimi yapabiliyor. Admin ise kullanıcı rollerini, ürün önerilerini ve katalog ürünlerini yönetebiliyor.

---

## 3. Gateway ve Discovery Katmanı

Sistemin giriş noktası olarak Gateway Service kullanıldı. Frontend, servislerin tek tek portlarına gitmek yerine API isteklerini gateway üzerinden gönderiyor. Gateway gelen JWT token'ı doğruluyor, kullanıcı bilgilerini header olarak ilgili servislere iletiyor ve böylece downstream servislerin tekrar tekrar JWT parse etmesine gerek kalmıyor.

Gateway tarafında özellikle şu sorumluluklar bulunuyor:

- İstekleri ilgili mikroservislere yönlendirme
- JWT doğrulama
- `X-User-Id`, `X-User-Role`, `X-User-Email` gibi güvenilir header'ları oluşturma
- Kullanıcıdan gelebilecek spoof edilmiş header'ları temizleme
- Rate limit için kullanıcı token'ı veya IP bazlı key üretme
- Servis erişilemezse fallback response döndürme

Discovery Service ise servislerin birbirini isim üzerinden bulabilmesi için kullanıldı. Örneğin bazı Feign Client kullanımlarında doğrudan URL yazmak yerine servis adı üzerinden Eureka discovery mekanizmasına güvenildi.

Bu katman sayesinde frontend daha sade kalırken backend servisleri de birbirinden bağımsız çalışabilecek şekilde konumlandırıldı.

---

## 4. Kullanıcı ve Yetkilendirme Akışı

User Service, sistemdeki kimlik doğrulama ve kullanıcı profili yönetiminden sorumludur. Kullanıcı kayıt olabilir, e-posta doğrulaması yapabilir, giriş yapabilir, refresh token ile access token yenileyebilir, şifre sıfırlama akışını kullanabilir ve profil/adres bilgilerini yönetebilir.

Kayıt akışı şu şekilde ilerler:

1. Kullanıcı frontend üzerinden kayıt formunu doldurur.
2. Frontend e-posta formatı geçerli olduğunda backend'e e-posta kullanım kontrolü yapar.
3. User Service kayıt sırasında e-posta çakışmasını kontrol eder.
4. Kullanıcı oluşturulur.
5. E-posta doğrulama token'ı üretilir.
6. Notification Service'e doğrulama e-postası için event gönderilir.
7. Kullanıcı doğrulama linkine tıkladığında hesabı aktif login akışına hazır hale gelir.

Login akışında kullanıcı e-posta ve şifre ile giriş yapar. Şifre doğrulaması Spring Security üzerinden yapılır. Hesap doğrulanmamışsa girişe izin verilmez ve frontend kullanıcıya doğrulama e-postasını yeniden gönderme imkanı sunar.

Projede roller üç temel seviyede tutuldu:

- **USER**: Standart müşteri
- **SELLER**: Satıcı paneline erişebilen kullanıcı
- **ADMIN**: Admin paneline erişebilen kullanıcı

Rol yönetimi admin paneli üzerinden yapılabiliyor. Admin, normal bir kullanıcıyı satıcı yapabiliyor ve isterse mağaza profilini de oluşturabiliyor.

---

## 5. Ürün, Kategori ve Marka Yönetimi

Product Service katalog ürünlerinden, kategorilerden, markalardan, ürün görsellerinden, satıcı listing'lerinden, ürün tekliflerinden ve yorumlardan sorumludur.

Bu projede ürün modeli iki katmanlı kurgulandı:

1. **Product**
   - Katalogdaki asıl ürün şablonudur.
   - Ürün adı, açıklama, kategori, marka, temel fiyat ve görseller burada tutulur.
   - Admin tarafından oluşturulabilir veya satıcı teklifinin admin tarafından onaylanmasıyla oluşabilir.

2. **ProductListing**
   - Satıcının katalogdaki bir ürünü kendi fiyatıyla satışa açmasıdır.
   - Satıcı ürün adı, kategori veya açıklama yazmaz; sadece mevcut katalog ürününe listing açar.
   - Böylece aynı ürün birden fazla satıcı tarafından farklı fiyatlarla listelenebilir.

Bu ayrım, gerçek e-ticaret platformlarına daha yakın bir yapı oluşturdu. Satıcıların kontrolsüz şekilde katalog kirliliği yaratması yerine admin moderasyonlu bir katalog akışı tercih edildi.

Admin tarafında şunlar yapılabilir:

- Kategori oluşturma/güncelleme/silme
- Marka oluşturma/güncelleme/silme
- Katalog ürünü oluşturma/güncelleme/silme
- Ürün görseli yükleme
- Satıcı ürün tekliflerini onaylama, reddetme veya revizyon isteme
- Onaylanan tekliften yeni katalog ürünü oluşturma
- Teklifi mevcut katalog ürününe bağlama

Satıcı tarafında şunlar yapılabilir:

- Yeni ürün önerisi gönderme
- Revizyon istenen öneriyi güncelleyip tekrar gönderme
- Onaylanan ürüne listing açma
- Listing fiyatını güncelleme
- Listing'i pasif/aktif yapma
- Listing'e bağlı stok bilgisini yönetme
- Ürün görselleri ekleme

Müşteri tarafında ise kullanıcı ürünleri listeleyebilir, filtreleyebilir, detaylarını görebilir, satıcı seçebilir, yorumları inceleyebilir ve ürünü sepete ekleyebilir.

---

## 6. Ürün Arama, Filtreleme ve Katalog Deneyimi

Ürün listeleme ekranında klasik filtreleme ve sayfalama desteği vardır. Kullanıcı şunlara göre filtreleme yapabilir:

- Anahtar kelime
- Kategori
- Marka
- Minimum fiyat
- Maksimum fiyat
- Sıralama alanı
- Sıralama yönü

Backend tarafında bu yapı dinamik sorgu üretimiyle desteklenmiştir. Kullanıcı hangi filtreleri gönderirse sadece o koşullar sorguya eklenir. Böylece her kombinasyon için ayrı repository metodu yazmak yerine daha esnek bir filtreleme yapısı oluşturuldu.

Frontend tarafında ürün listeleme ekranı hem desktop hem mobil kullanım için tasarlandı. Desktop'ta sidebar filtreleme, mobilde açılır filtre paneli kullanıldı.

Ayrıca AI destekli semantik arama modu da eklendi. Kullanıcı doğal dille arama yaptığında search/recommendation servisinden ürün ID'leri alınarak ilgili ürünler katalogdan çekiliyor. Böylece klasik keyword search dışında anlam bazlı ürün keşfi de sağlandı.

---

## 7. Sepet Akışı

Cart Service, alışveriş sepetini Redis üzerinde tutar. Sepet geçici ve sık değişen bir veri olduğu için ilişkisel veritabanı yerine Redis tercih edildi.

Sepet akışı şu şekilde ilerler:

1. Kullanıcı ürün detay sayfasında bir satıcı listing'i seçer.
2. Seçilen listing'e ait fiyat ve seller bilgisiyle ürün sepete eklenir.
3. Cart Service bu bilgiyi Redis Hash yapısında saklar.
4. Kullanıcı adet artırabilir, azaltabilir veya ürünü sepetten çıkarabilir.
5. Sepet toplam adet ve toplam tutar bilgisi frontend'de anlık gösterilir.
6. Sipariş oluşturulduktan sonra sepet temizlenir.

Sepette ürünün `productId`, `listingId`, `sellerId`, ürün adı, birim fiyatı ve miktarı tutulur. Bu yapı, ürünün hangi satıcıdan alındığını sipariş sürecine taşıyabilmek için önemlidir.

---

## 8. Stok Yönetimi

Stock Service, ürünlerin stoklarını ve stok hareketlerini yönetir. Başlangıçta ürün bazlı stok yapısı desteklenirken proje içinde satıcıya özgü stok yapısına da genişletildi. Böylece aynı ürün için farklı satıcıların ayrı stokları olabilir.

Stok tarafında üç temel kavram kullanıldı:

- **quantity**: Toplam stok
- **reservedQty**: Sipariş oluşturulmuş ama henüz kesinleşmemiş rezerve miktar
- **availableQty**: Satılabilir miktar

Sipariş oluşturulduğunda stok doğrudan düşülmez. Önce rezerve edilir. Ödeme başarılı olursa rezervasyon kesinleşir ve stoktan düşer. Ödeme başarısız olursa rezervasyon iade edilir. Bu yaklaşım, ödeme sürecinde stok tutarlılığını korumak için kullanıldı.

Stok hareketleri ayrıca audit trail olarak kaydedilir. Böylece stok girişleri, rezervasyonlar, iadeler, onaylar ve manuel düzeltmeler takip edilebilir.

---

## 9. Sipariş ve Saga Akışı

Order Service, sipariş yaşam döngüsünün merkezindeki servistir. Sipariş oluşturma, sipariş durumlarını güncelleme ve diğer servislerden gelen eventlere göre sipariş akışını ilerletme görevini üstlenir.

Genel sipariş akışı şu şekildedir:

1. Kullanıcı checkout ekranında adres bilgilerini seçer veya yeni adres girer.
2. Frontend, sepet kalemleriyle birlikte Order Service'e sipariş oluşturma isteği gönderir.
3. Order Service siparişi başlangıç durumuyla oluşturur.
4. Stock Service'e stok rezervasyonu için event gönderilir.
5. Stock Service stok yeterliyse rezervasyon yapar ve başarılı event döner.
6. Order Service stok rezerve edildi bilgisini alınca siparişi ödeme aşamasına taşır.
7. Payment Service ödeme formunu veya sandbox ödeme sürecini başlatır.
8. Ödeme başarılıysa Order Service'e payment success event'i gelir.
9. Order Service siparişi onaylar ve kargo oluşturma event'i gönderir.
10. Cargo Service takip numarası oluşturur ve Order Service'e bildirir.
11. Sipariş kargoya verilir ve teslim edildiğinde sipariş tamamlanır.

Bu yapı distributed transaction yerine event-driven Saga yaklaşımına yakındır. Her servis kendi sorumluluğunu yerine getirir ve sonraki adım eventler üzerinden tetiklenir.

---

## 10. Ödeme Akışı

Payment Service, Iyzico entegrasyonu için ayrıldı. Kullanıcı checkout sonrası ödeme sayfasına yönlendirilir. Payment Service siparişe ait ödeme durumunu takip eder ve frontend ödeme formu / ödeme durumu bilgilerini bu servisten alır.

Projede sandbox kullanımına uygun şekilde ödeme simülasyonu da eklendi. Local geliştirme ortamında Iyzico callback'i her zaman doğrudan uygulamaya ulaşamayacağı için, başarılı ödeme simülasyonu yapılabilecek bir endpoint eklendi. Böylece demo sırasında sipariş akışının ödeme sonrası kargo aşamasına ilerlemesi gösterilebiliyor.

Ödeme akışında temel hedef, sipariş oluşturma ile ödeme onayını ayrıştırmak ve ödeme başarılı olmadan stoğun kesin olarak düşmemesini sağlamaktı.

---

## 11. Kargo Akışı

Cargo Service, sipariş onaylandıktan sonra mock kargo oluşturma ve takip bilgisi üretme görevini üstlenir. Sipariş için takip numarası oluşturulur ve kargo eventleri üzerinden Order Service bilgilendirilir.

Takip numarası üretimi restart sonrası çakışma olmaması için veritabanı destekli sequence mantığıyla tasarlandı. Bu sayede uygulama yeniden başlasa bile aynı gün içinde aynı takip numarası tekrar üretilmez.

Frontend tarafında kullanıcı sipariş detay sayfasında kargo durumunu ve takip hareketlerini görebilir. Bu da sipariş sürecinin sadece ödeme ile bitmediğini, teslimat aşamasına kadar takip edilebilir olduğunu gösterir.

---

## 12. Notification Service

Notification Service, sistemdeki e-posta gönderim sorumluluğunu ayrı bir servis olarak konumlandırır. User Service doğrudan e-posta göndermek yerine event yayınlar. Notification Service bu eventleri dinleyerek ilgili e-postaları gönderir.

Bu servis özellikle şu akışlarda kullanılır:

- Kullanıcı kayıt sonrası e-posta doğrulama
- Şifre sıfırlama e-postası
- Kargo/sipariş akışı e-postası

E-posta gönderiminde hem düz metin hem de Thymeleaf tabanlı HTML şablon desteği vardır. Bu sayede notification logic, user veya order servislerinin içine gömülmeden ayrı tutulmuş oldu.

---

## 13. AI Assistant ve Akıllı Özellikler

Nice-to-have kapsamında projeye AI destekli ayrı bir **Assistant Service** ekledim. Bu servisi yalnızca “chatbot cevap versin” mantığında değil, backend servisleriyle konuşabilen ve gerektiğinde canlı sistem verisini araç olarak kullanabilen bir asistan mimarisi şeklinde kurguladım.

Assistant Service’in temel amacı, kullanıcıların klasik arama, filtreleme veya sipariş takip ekranlarına gitmeden doğal dil ile işlem yapabilmesini sağlamaktır. Kullanıcı “gaming laptop önerir misin?”, “ORD-... numaralı siparişim nerede?”, “iade politikanız nedir?” gibi sorular sorduğunda asistan yalnızca model bilgisinden cevap üretmez; uygun durumda backend servislerinden veri çekerek cevap oluşturur.

Bu yapı için Assistant Service içinde tool yaklaşımı kullandım. AI modeline her soruda doğrudan serbest cevap üretmek yerine, belirli işlevleri temsil eden araçlar tanımladım. Böylece model, kullanıcının niyetine göre ürün arama, ürün detayını getirme, sipariş sorgulama, öneri alma veya platform politikası açıklama gibi işlemleri tetikleyebilir.

AI Assistant tarafında desteklenen başlıca yetenekler şunlardır:

- Ürün arama
- Ürün detay sorgulama
- Sipariş bilgisi sorgulama
- Kullanıcıya özel öneri akışını tetikleme
- Platform politikaları hakkında cevap üretme
- Ürün açıklaması üretme
- Ürün görseli üretme
- Chat session geçmişini Redis’te saklama
- Konuşma boyunca bulunan ürünleri frontend’e kart olarak döndürme

Burada önemli nokta, asistanın cevaplarının backend verilerine dayanmasıdır. Örneğin ürün önerisi veya ürün arama sorularında asistan, ürün kataloğu ve recommendation/search servislerinden gelen sonuçları kullanır. Sipariş sorgularında ise kullanıcının kimliği gateway üzerinden gelen `X-User-Id` bilgisiyle taşındığı için başka bir kullanıcının siparişine erişilmemesi hedeflenmiştir.

Asistan tarafında konuşma geçmişi Redis’te tutulur. Böylece kullanıcı aynı session içinde devam eden bir konuşma yaptığında önceki mesajlar kaybolmaz. Bu, özellikle “biraz daha ucuz olanları göster”, “bunun benzeri var mı?” gibi bağlam gerektiren konuşmalarda daha gerçekçi bir deneyim sağlar. Chat session temizleme endpoint’i ile kullanıcı konuşmayı sıfırlayabilir.

AI Assistant’ın frontend tarafındaki karşılığı da sadece düz metin cevap gösteren bir chat ekranı değildir. Asistan bir ürün arama veya öneri sonucu bulduğunda, bu ürünler frontend’e ayrıca `products` alanı içinde döndürülür ve chat ekranında ürün kartları olarak gösterilir. Bu sayede kullanıcı AI cevabından doğrudan ürün detay sayfasına geçebilir. Yani AI çıktısı yalnızca metin değil, uygulama içi aksiyona dönüşebilen bir sonuçtur.

Admin ve satıcı tarafında ise gen AI özellikleri eklendi. Admin bir ürün teklifini onaylarken AI ile ürün açıklaması oluşturabiliyor veya ürün görseli üretebiliyor. Satıcı da listing yönetimi sırasında AI destekli ürün görseli üretme akışını kullanabiliyor. Üretilen görsel URL’si geçici olabileceği için backend tarafında bu görsel Cloudinary’ye yüklenerek kalıcı hale getiriliyor. Bu kısımda dalle modeli kullanıldı bu model performans ve kalite olarak inanılmaz düşük bir seviyede olmasına rağmen, maliyet olarak çok uygundur, projeye tamamen ekstra olarak eklenmiştir. Normal şartlarda bir satış platformunda ai image generation olması kullanıcı deneyimi açısından inanılmaz olumsuzdur. Rahatça product image'ları doldurabilmemi ve test yapabilmemi sağladı bu ekleme.

Genel olarak bu yapıyı projeye özellikle nice-to-have olarak ekledim çünkü klasik bir bootcamp e-ticaret projesinde beklenen CRUD, sepet ve sipariş akışlarının üzerine daha modern bir AI destekli alışveriş deneyimi koyuyor. Aynı zamanda tool kullanımı, Redis session yönetimi, servisler arası veri çekme ve frontend’de AI sonuçlarını kart olarak gösterme gibi konularla mimari açıdan daha güçlü bir katman oluşturuyor.

---

## 14. Recommendation, Neo4j ve Semantic Search Deneyimi

Projede klasik ürün listeleme ve filtrelemenin yanında ayrı bir **search / recommendation deneyimi** de ekledim. Bu bölümün amacı, kullanıcının sadece kategori, marka ve fiyat filtresiyle ürün bulmasını değil; davranış bazlı öneriler, popüler ürünler, benzer ürünler ve doğal dilde semantik arama üzerinden ürün keşfetmesini sağlamaktır.

Bu kapsamda iki ayrı akış kurguladım:

1. **Neo4j tabanlı öneri akışı**
2. **Semantic search tabanlı ürün arama akışı**

Neo4j tarafında ürünler ve kullanıcı davranışları graph mantığıyla ele alınır. Kullanıcının ürün görüntüleme veya satın alma gibi davranışları graph üzerinde ilişki olarak düşünülebilir. Bu yaklaşım, klasik ilişkisel sorgulardan farklı olarak “bu ürüne bakanlar başka hangi ürünlere baktı?”, “bu ürünü alanlar ne aldı?”, “bu kullanıcının geçmiş davranışlarına göre hangi ürünler yakın olabilir?” gibi ilişki temelli öneriler üretmeye daha uygundur.

Frontend’de bunun birkaç karşılığı vardır. Ana sayfada kullanıcı giriş yaptıysa kişiselleştirilmiş öneriler gösterilmeye çalışılır. Eğer kullanıcıya özel öneri üretilemezse veya henüz yeterli davranış verisi yoksa popüler ürünlere fallback yapılır. Bu sayede öneri alanı boş kalmaz ve kullanıcı yine anlamlı bir ürün listesi görür.

Ürün detay sayfasında da benzer ürün / birlikte alınan ürün yaklaşımı kullanıldı. Kullanıcı bir ürün detayına girdiğinde frontend, ilgili ürün için recommendation endpoint’ini çağırır ve dönen ürün ID’leri üzerinden ürün detaylarını çeker. Böylece ürün detay sayfasında “Bunu Alanlar Bunu da Aldı” benzeri bir alan gösterilir. Bu alan, e-ticaret platformlarında sık kullanılan graph tabanlı keşif mantığını demo seviyesinde temsil eder.

Ayrıca ürün görüntüleme davranışı da takip edilir. Kullanıcı ürün detay sayfasına girdiğinde frontend fire-and-forget şekilde view tracking çağrısı yapar. Bu çağrı recommendation tarafında kullanıcı-ürün ilişkilerinin güncellenmesine temel oluşturur. Böylece sistem yalnızca statik ürün listesi sunmak yerine, kullanıcının davranışlarından öğrenebilecek bir yapıya hazırlanmış olur.

Fallback mekanizması özellikle bu akışta önemliydi. Çünkü demo ortamında her kullanıcı için yeterli graph verisi oluşmayabilir. Bu durumda sistem tamamen boş sonuç göstermek yerine popüler ürünlere döner. Böylece öneri bileşenleri hem giriş yapmış hem de giriş yapmamış kullanıcılar için çalışır durumda kalır. Giriş yapmamış kullanıcıya popüler ürünler gösterilirken, giriş yapmış kullanıcı için önce kişiselleştirilmiş öneri denenir; sonuç yoksa yine popüler ürünlere fallback yapılır.

Semantic search tarafında ise klasik keyword search’ün ötesine geçildi. Normal ürün listeleme ekranında kullanıcı kategori, marka, fiyat ve keyword filtreleriyle arama yapabilir. Buna ek olarak AI/semantic search endpoint’i ile doğal dil sorgularından ürün ID’leri alınır. Örneğin kullanıcı “gaming laptop 30 bin altı”, “gürültü engelleyen kulaklık”, “hafif seyahat laptopu” gibi daha serbest ifadelerle arama yapabilir. Bu noktada amaç, ürün adında birebir aynı kelimeler geçmese bile anlam olarak yakın ürünleri bulabilmektir.

Frontend’de semantic search akışı şöyle çalışır:

1. Kullanıcı doğal dilde bir arama yapar.
2. Frontend search/recommendation servisine sorguyu gönderir.
3. Servis ilgili ürün ID’lerini döndürür.
4. Frontend bu ID’ler için Product Service’ten ürün detaylarını çeker.
5. Sonuçlar normal ürün kartlarıyla listelenir.

Bu yapı sayesinde search servisi ürün ID’si döndürmekle sınırlı kalır, ürünün güncel fiyatı, adı, görselleri ve kategori bilgisi yine Product Service’ten alınır. Böylece semantic search sonucu eski veya eksik ürün bilgisi taşımak yerine güncel katalog verisiyle birleşir.

Semantic search başarısız olursa veya servis beklenen sonucu dönemezse frontend tarafında klasik ürün aramasına fallback yapılır. Yani semantic search çağrısı hata alırsa ürün listesi tamamen boşa düşmez; aynı sorgu keyword bazlı ürün aramada denenir. Bu da demo sırasında sistemin daha dayanıklı görünmesini sağlar.

AI Assistant da bu search ve recommendation yapısıyla entegre çalışır. Kullanıcı chat ekranından ürün önerisi istediğinde asistan, tanımlı tool’lar üzerinden ürün arama veya öneri servislerini kullanabilir. Eğer ürün sonucu bulunursa yalnızca metin cevap üretmekle kalmaz, bulunan ürünleri frontend’e kart formatında döndürür. Bu, AI Assistant’ı klasik bir sohbet botundan çıkarıp e-ticaret aksiyonlarını başlatabilen bir alışveriş yardımcısına dönüştürür.

Özetle bu bölümde üç katmanlı bir akıllı ürün keşfi yapısı kurdum:

- **Klasik filtreleme:** Product Service üzerinden kategori, marka, fiyat ve keyword bazlı listeleme.
- **Semantic search:** Doğal dil sorgularından anlam bazlı ürün bulma.
- **Neo4j recommendation:** Kullanıcı davranışı ve ürün ilişkileri üzerinden öneri üretme.

---

## 15. Frontend Mimarisi

Frontend React + TypeScript ile geliştirildi. Sayfalar müşteri, satıcı, admin ve auth alanlarına ayrıldı.

Başlıca frontend bölümleri:

- **Auth Pages**
  - Login
  - Register
  - Email verification
  - Forgot password
  - Reset password

- **Customer Pages**
  - Home
  - Products
  - Product detail
  - Cart
  - Checkout
  - Payment
  - Orders
  - Order detail
  - Profile
  - AI Chat

- **Seller Pages**
  - Seller dashboard
  - Listing management
  - Product proposal management

- **Admin Pages**
  - Admin dashboard
  - User role management
  - Product proposal moderation
  - Product management

State yönetimi için Zustand kullanıldı. Auth bilgisi persist edildi, cart state ise global store üzerinden yönetildi. API çağrıları için Axios client oluşturuldu ve JWT otomatik olarak isteklere eklendi. Access token süresi dolduğunda refresh token ile yenileme yapılacak şekilde interceptor yazıldı.

Veri çekme, cache ve invalidation için React Query kullanıldı. Böylece ürün listeleri, sepet, siparişler, admin listeleri ve seller panelleri daha kontrollü şekilde yönetildi.

UI tarafında shadcn tarzı reusable component yapısı kullanıldı. Button, Card, Dialog, Sheet, Tabs, Table, Input, Select gibi ortak componentler ayrıldı. Bu, hem tasarımı tutarlı hale getirdi hem de sayfaları daha okunabilir yaptı.

---

## 16. Redis-backed Bloom Filter ile E-posta Kontrol Optimizasyonu

Projeye nice-to-have olarak Redis destekli bir Bloom Filter yapısı da ekledim. Bloom Filter algoritmasını çok beğendiğim fakat daha önce kullanma fırsatım olmadığı için eklemek istedim.

Bloom Filter’ı kullanıcı kayıt akışında e-posta adresinin sistemde kayıtlı olup olmadığını hızlıca kontrol etmek için kullandım. Normal şartlarda her kayıt denemesinde doğrudan veritabanına `existsByEmail` sorgusu atılabilir. Küçük sistemlerde bu problem yaratmaz; fakat kullanıcı sayısı arttıkça ve özellikle kayıt ekranında sık e-posta kontrolü yapılmaya başlandıkça bu sorgular veritabanı üzerinde gereksiz yük oluşturabilir.

Bu nedenle kayıt akışında önce Bloom Filter’a bakıyorum:

1. Eğer Bloom Filter “bu e-posta kesinlikle yok” derse, kayıt akışı DB’ye ekstra kontrol sorgusu atmadan devam edebilir.
2. Eğer Bloom Filter “bu e-posta muhtemelen var” derse, kesin karar vermek için veritabanına doğrulama sorgusu atılır.
3. Böylece yanlış pozitif ihtimali güvenli şekilde DB ile doğrulanır.
4. Yeni kullanıcı başarıyla kaydedildiğinde e-posta Bloom Filter’a da eklenir.

Bloom Filter’ın önemli özelliği, **false negative üretmemesidir**. Yani filtrede gerçekten var olan bir e-postaya “kesinlikle yok” demesi beklenmez. Ancak **false positive** üretebilir; yani aslında sistemde olmayan bir e-posta için “muhtemelen var” diyebilir. Bu yüzden Bloom Filter sonucu tek başına kullanıcıya hata döndürmek için kullanılmaz; “muhtemelen var” sonucunda mutlaka DB doğrulaması yapılır.

Bu projede Bloom Filter, yaklaşık **1.000.000 kullanıcı beklentisi** ve **%1 false positive oranı** hedeflenerek kurgulandı. Bu yaklaşım, çok düşük bellek kullanımıyla yüksek hızlı üyelik kontrolü sağlamayı amaçlar. Bloom Filter bit dizisi uygulama belleğinde tutulurken, servis restart olduğunda tekrar kullanılabilmesi için serialized hali Redis’te saklandı.

Redis burada kalıcı bir yardımcı cache gibi kullanıldı:

- Uygulama ayağa kalkarken önce Redis’te daha önce oluşturulmuş Bloom Filter var mı diye kontrol edilir.
- Redis’te kayıtlı bit dizisi varsa deserialize edilerek memory’ye yüklenir.
- Redis’te yoksa mevcut kullanıcı e-postaları DB’den okunur ve Bloom Filter baştan oluşturulur.
- Yeni kullanıcı kaydedildiğinde memory’deki Bloom Filter güncellenir.
- Güncellenen filter async olarak Redis’e tekrar yazılır.

Bu yapı sayesinde uygulama her restart olduğunda tüm kullanıcı e-postalarını DB’den okuyarak Bloom Filter oluşturmak zorunda kalmaz. Redis’teki serialized bit dizisi doğrudan yüklenebilir.


Özetle Bloom Filter’ı bu projede zorunlu bir gereksinim olarak değil, ölçeklenebilir sistemlerde sık kullanılan verimli bir membership-check algoritmasını göstermek için ekledim. Demo proje olmasına rağmen, hem Redis entegrasyonu hem de DB yükünü azaltma fikrini göstermesi açısından teknik olarak değerli bir nice-to-have özellik olduğunu düşünüyorum.

---

## 17. Eklenen Nice-to-Have Özellikler

Temel gereksinimlerin dışında projeye birçok ekstra özellik ekledim.

Eklenen nice-to-have özellikler:

- Redis-backed Bloom Filter ile e-posta kontrol optimizasyonu
- Register ekranında debounce'lu anlık e-posta uygunluk kontrolü
- E-posta doğrulanmadıysa login ekranından yeniden doğrulama maili gönderme
- Refresh token interceptor ve otomatik token yenileme
- Redis tabanlı sepet yönetimi
- Sepete TTL eklenmesi
- Satıcı bazlı listing modeli
- Satıcı bazlı stok modeli
- Stok rezervasyon / release / confirm akışı
- Stock movement audit trail
- Event-driven Saga benzeri sipariş akışı
- Iyzico sandbox ödeme simülasyonu
- Mock kargo takip sistemi
- DB-backed tracking number generator
- Notification service ile e-posta sorumluluğunu ayırma
- AI alışveriş asistanı
- AI ürün açıklaması üretimi
- AI ürün görseli üretimi
- Semantik ürün arama
- Kişisel / popüler öneri alanları
- Ürün detayında benzer ürün önerileri
- Admin teklif onayında mevcut ürüne bağlama seçeneği
- Admin teklif onayında yeni ürün için görsel yükleme
- Satıcı mağaza profili
- Admin'in kullanıcıyı satıcı yaparken mağaza profili oluşturabilmesi
- Frontend'de ayrı customer/seller/admin layout yapısı
- Mobil uyumlu filtre paneli
- Toast bildirimleri
- Skeleton loading state'leri
- Error boundary
- Swagger dokümantasyonu
- Reusable UI component yapısı

---