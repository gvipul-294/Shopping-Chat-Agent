package com.example.agent.model;

import java.util.List;

public class ChatResponse {
    private String message;
    private List<PhoneRecommendation> recommendations;
    private List<Phone> comparisonPhones;
    private SafetyResult safetyResult;
    private String conversationId;
    private String intent;

    public ChatResponse() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PhoneRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<PhoneRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public List<Phone> getComparisonPhones() {
        return comparisonPhones;
    }

    public void setComparisonPhones(List<Phone> comparisonPhones) {
        this.comparisonPhones = comparisonPhones;
    }

    public SafetyResult getSafetyResult() {
        return safetyResult;
    }

    public void setSafetyResult(SafetyResult safetyResult) {
        this.safetyResult = safetyResult;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }
}

