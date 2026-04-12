package com.example.CourseWork.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class WebSocketConfigTest {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Test
    void contextLoads_ShouldHaveMessagingTemplate() {
        // Verify that WebSocket support is enabled and template is available
        assertNotNull(messagingTemplate, "SimpMessagingTemplate should be available in context");
    }

    @Test
    void configIsPresent_ShouldLoadWebSocketConfig() {
        assertNotNull(webSocketConfig, "WebSocketConfig should be bean");
    }
}
