package org.example.pump_screener.config;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("application.properties")
public class BinanceConfig {
    @Value("${api_key}")
    private String apiKey;

    @Value("${secret_key}")
    private String secretKey;

    @Bean
    public SpotClient spotClient() {
        return new SpotClientImpl(apiKey, secretKey);
    }
}