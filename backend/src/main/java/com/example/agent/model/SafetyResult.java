package com.example.agent.model;

public class SafetyResult {
    private boolean isSafe;
    private String reason;
    private String sanitizedMessage;

    public SafetyResult() {
        this.isSafe = true;
    }

    public SafetyResult(boolean isSafe, String reason) {
        this.isSafe = isSafe;
        this.reason = reason;
    }

    public boolean isSafe() {
        return isSafe;
    }

    public void setSafe(boolean safe) {
        isSafe = safe;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSanitizedMessage() {
        return sanitizedMessage;
    }

    public void setSanitizedMessage(String sanitizedMessage) {
        this.sanitizedMessage = sanitizedMessage;
    }
}

