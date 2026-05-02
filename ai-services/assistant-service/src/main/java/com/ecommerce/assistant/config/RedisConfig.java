package com.ecommerce.assistant.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis konfigürasyonu.
 *
 * Bu sınıf, Assistant Service içinde kullanılacak özel RedisTemplate bean'ini oluşturur.
 *
 * Neden özel RedisTemplate gerekiyor:
 *
 * AssistantServiceImpl konuşma geçmişini Redis içinde saklar.
 * Bu konuşma geçmişi sadece String değerlerden oluşmayabilir.
 * Örneğin:
 * - sessionId
 * - kullanıcı mesajları
 * - assistant cevapları
 * - tarih/saat bilgileri vs.
 *
 * Bu yüzden RedisTemplate<String, Object> kullanılır.
 *
 * Spring Boot normalde Redis için varsayılan bir RedisTemplate oluşturuyor
 * fakat bildiğim kadarıyla bu template String için çalışıyor.
 * Bu yüzden özel serializer tanımlamam gerekti.
 *
 * Bu sınıfta yapılan işlem:
 *
 * 1. ObjectMapper oluşturulur.
 *
 *      ObjectMapper mapper = new ObjectMapper();
 *
 *    ObjectMapper, Java nesnelerini JSON'a
 *    JSON verilerini de tekrar Java nesnelerine çevirmek için kullanılır
 *
 * 2. JavaTimeModule eklenir
 *
 *      mapper.registerModule(new JavaTimeModule());
 *
 *    LocalDateTime, LocalDate, Instant gibi tarih/saat bilgilerini serialize edebilmek için ekliyoruz.
 *
 * 3. Default typing aktif edilir.
 *
 *      mapper.activateDefaultTyping(...);
 *
 *    Redis'e Object tipinde veri yazdığımız için Jackson'ın gerçek sınıf bilgisini
 *    JSON içine eklemesi gerekir.
 *
 *    Örneğin Redis'e bir ArrayList, HashMap veya özel DTO yazıldığında,
 *    geri okurken bunun hangi Java tipine dönüştürüleceğini Jackson bilmelidir.
 *
 *    ObjectMapper.DefaultTyping.NON_FINAL:
 *    final olmayan sınıflar için type bilgisi eklenir.
 *
 *    JsonTypeInfo.As.PROPERTY:
 *    type bilgisi JSON içinde ayrı bir property olarak tutulur.
 *
 *
 * 4. Jackson2JsonRedisSerializer oluşturulur.
 *
 *      Jackson2JsonRedisSerializer<Object> serializer =
 *          new Jackson2JsonRedisSerializer<>(mapper, Object.class);
 *
 *    Bu serializer, Redis'e yazılacak Object değerlerini JSON formatına çevirir.
 *    Redis'ten okunurken de JSON'u tekrar Java nesnesine dönüştürür.
 *
 * 5. RedisTemplate oluşturulur.
 *
 *      RedisTemplate<String, Object> template = new RedisTemplate<>();
 *
 *    RedisTemplate, Redis üzerinde get, set, delete, expire gibi işlemleri
 *    Java kodundan yapmamızı sağlayan Spring sınıfıdır.
 *
 * 6. Redis bağlantı factory'si template'e verilir.
 *
 *      template.setConnectionFactory(factory);
 *
 *    RedisConnectionFactory, Redis sunucusuna bağlantı oluşturur.
 *    Spring Boot bunu application.yml / environment ayarlarına göre hazırlar.
 *
 * 7. Key serializer ayarlanır.
 *
 *      template.setKeySerializer(new StringRedisSerializer());
 *
 *    key'lerin okunabilir kalması için kullanıyoruz.
 *
 * 8. Hash key serializer ayarlanır.
 *
 *      template.setHashKeySerializer(new StringRedisSerializer());
 *
 *    redis hash kullanılırsa hash içindeki field isimleri de string olarak saklanır.
 *
 * 9. Value serializer ayarlanır.
 *
 *      template.setValueSerializer(serializer);
 *
 *    Normal Redis value alanları JSON olarak serialize edilir.
 *
 * 10. Hash value serializer ayarlanır.
 *
 *      template.setHashValueSerializer(serializer);
 *
 *    Redis Hash içindeki value alanları da JSON olarak serialize edilir.
 *
 * 11. Template initialize edilir.
 *
 *      template.afterPropertiesSet();
 *
 *    Tüm serializer ve connection ayarları tamamlandıktan sonra RedisTemplate kullanıma hazır hale getirilir.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
