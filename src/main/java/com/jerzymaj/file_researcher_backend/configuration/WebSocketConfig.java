package com.jerzymaj.file_researcher_backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        messageBrokerRegistry.enableSimpleBroker("/topic"); // wyjście (lub wewnętrzna skrzynka odbiorcza klienta).
        messageBrokerRegistry.setApplicationDestinationPrefixes("/app"); // wejście do aplikacji (nie używane, standardowa konfiguracja)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        stompEndpointRegistry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // todo ustawić zmienną w pliku konfiguracyjnym aplikacji
                .withSockJS(); // włącza ona bibliotekę SockJS
//        Jeśli użytkownik łączy się z sieci korporacyjnej, która blokuje WebSockety
//                SockJS automatycznie przełączy się na inną metodę (np. HTTP Long Polling), udając, że to WebSocket.
    }
}
