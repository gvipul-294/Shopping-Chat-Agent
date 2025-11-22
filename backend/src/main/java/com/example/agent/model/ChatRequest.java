package com.example.agent.model;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
    
    private String conversationId;
    private String context;

    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}

