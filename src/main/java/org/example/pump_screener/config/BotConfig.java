package org.example.pump_screener.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("application.properties")
public class BotConfig {
    @Value("${bot_name}")
    String bot_name;

    @Value("${bot_token}")
    String bot_token;

}
