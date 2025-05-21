package frba.utn.edu.ar.tp_tacs.api.telegramBot.webClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        //TODO cambiarlo en prod
        return WebClient.builder().baseUrl("http://localhost:8080").build();
    }
}
