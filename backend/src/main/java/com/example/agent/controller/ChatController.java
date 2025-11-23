package com.example.agent.controller;

import com.example.agent.model.ChatRequest;
import com.example.agent.model.ChatResponse;
import com.example.agent.service.ConversationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        logger.info("Received chat request - conversationId: {}, message length: {}", 
                request.getConversationId(), request.getMessage() != null ? request.getMessage().length() : 0);
        try {
            ChatResponse response = conversationService.processMessage(request);
            logger.debug("Chat response generated - intent: {}, has recommendations: {}", 
                    response.getIntent(), response.getRecommendations() != null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing chat message", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "shopping-chat-agent");
        return ResponseEntity.ok(health);
    }
}

