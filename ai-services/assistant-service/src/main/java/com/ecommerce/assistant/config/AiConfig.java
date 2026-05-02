package com.ecommerce.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient konfigürasyonu.
 *
 * Bu sınıf, Assistant Service içinde kullanılacak ChatClient bean'ini oluşturur.
 *
 * ChatClient:
 * LLM'e mesaj göndermek ve modelden cevap almak için kullanılır.
 *
 * Bu projede ChatClient, OpenAI modeline istek atmak için kullanılır.
 *
 * Akış:
 *
 * 1. Uygulama ayağa kalkarken Spring bu configuration sınıfını okur.
 *
 * 2. chatClient() metodu bir @Bean olarak çalışır.
 *
 * 3. Spring AI, ChatClient.Builder nesnesini otomatik olarak hazırlar. 
 * Bu builder, application.yml / environment değişkenlerindeki AI ayarlarına göre konfigüre edilir.
 *
 * 4. builder.build() çağrısı ile ChatClient nesnesi oluşturulur.
 *
 * 5. Oluşturulan ChatClient Spring container'a bean olarak eklenir.
 *
 * 6. Daha sonra service sınıflarında constructor injection ile kullanılabilir.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
