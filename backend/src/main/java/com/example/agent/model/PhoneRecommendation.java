package com.example.agent.model;

import java.util.List;

public class PhoneRecommendation {
    private Phone phone;
    private String rationale;
    private Double relevanceScore;

    public PhoneRecommendation() {}

    public PhoneRecommendation(Phone phone, String rationale) {
        this.phone = phone;
        this.rationale = rationale;
    }

    public Phone getPhone() {
        return phone;
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}

