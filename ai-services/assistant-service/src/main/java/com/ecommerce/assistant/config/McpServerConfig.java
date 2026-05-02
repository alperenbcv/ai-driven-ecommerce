package com.ecommerce.assistant.config;

import com.ecommerce.assistant.service.EcommerceTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class McpServerConfig {

    /**
     * ToolCallbackProvider: @Tool annotasyonlu metodları tarayıp
     * Spring AI MCP Server'a kayıt eder.
     *
     * Autoconfiguration (McpWebMvcServerAutoConfiguration) bu bean'i bulur
     * ve /mcp/sse endpoint'ine bağlar. Bizim ekstra kod yazmamıza gerek kalmaz.
     */
    @Bean
    public ToolCallbackProvider ecommerceToolCallbackProvider(EcommerceTools ecommerceTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(ecommerceTools)
                .build();
    }
}
