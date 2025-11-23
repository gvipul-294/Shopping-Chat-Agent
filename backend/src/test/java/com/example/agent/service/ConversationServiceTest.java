package com.example.agent.service;

import com.example.agent.model.ChatRequest;
import com.example.agent.model.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "openai.api.key=dummy",
        "openai.api.model=gpt-3.5-turbo"
})
class ConversationServiceTest {

    @Autowired
    private ConversationService conversationService;

    @Test
    void testProcessMessageWithPriceQuery() {
        ChatRequest request = new ChatRequest("Show me phones under 30000");
        ChatResponse response = conversationService.processMessage(request);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertNotNull(response.getConversationId());
        assertNotNull(response.getIntent());
        assertEquals("search_by_price", response.getIntent());
    }

    @Test
    void testProcessMessageWithBrandQuery() {
        ChatRequest request = new ChatRequest("Show me Samsung phones");
        ChatResponse response = conversationService.processMessage(request);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals("search_by_brand", response.getIntent());
    }

    @Test
    void testProcessMessageWithRecommendation() {
        ChatRequest request = new ChatRequest("Recommend a good phone");
        ChatResponse response = conversationService.processMessage(request);

        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals("recommend", response.getIntent());
    }

    @Test
    void testProcessMessageGeneratesConversationId() {
        ChatRequest request = new ChatRequest("Hello");
        ChatResponse response = conversationService.processMessage(request);

        assertNotNull(response.getConversationId());
        assertFalse(response.getConversationId().isEmpty());
    }

    @Test
    void testProcessMessageWithSafetyCheck() {
        ChatRequest request = new ChatRequest("This is a very long message that exceeds the maximum allowed length and should be rejected by the safety check system because it contains too many characters and might be considered spam or abuse of the system");
        // The message is long but under 1000 chars, so it should pass
        ChatResponse response = conversationService.processMessage(request);

        assertNotNull(response);
        assertTrue(response.getSafetyResult().isSafe());
    }
}

