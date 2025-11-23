package com.example.agent.service;

import com.example.agent.model.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private PhoneCatalogService phoneCatalogService;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String openaiModel;

    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    private volatile OpenAiService openAiService;

    // Patterns for intent detection
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:under|below|less than|max|maximum|budget|price|₹|\\$|rs|rupees?)\\s*(?:of\\s*)?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRAND_PATTERN = Pattern.compile("\\b(OnePlus|Google|Pixel|Samsung|Xiaomi|Redmi|Nothing|Realme|Vivo|Motorola)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FEATURE_PATTERN = Pattern.compile("\\b(fast charging|AMOLED|OLED|120Hz|camera|battery|storage|ram|processor|OIS|water resistant|AI features)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPARE_PATTERN = Pattern.compile("\\b(compare|comparison|difference|vs|versus|between)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RECOMMEND_PATTERN = Pattern.compile("\\b(recommend|suggest|best|good|which|what)\\b", Pattern.CASE_INSENSITIVE);

    @PostConstruct
    public void init() {
        logger.info("Initializing ConversationService");
        // Initialize OpenAI service if API key is available
        initializeOpenAiService();
        if (openAiService != null) {
            logger.info("OpenAI service initialized successfully");
        } else {
            logger.warn("OpenAI service not initialized - API key not available. Fallback responses will be used.");
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down ConversationService");
        // Properly shutdown OpenAI service to clean up resources
        if (openAiService != null) {
            openAiService.shutdownExecutor();
            logger.info("OpenAI service shutdown complete");
        }
    }

    private synchronized void initializeOpenAiService() {
        if (openAiService == null && openaiApiKey != null && !openaiApiKey.isEmpty() && !openaiApiKey.equals("dummy")) {
            openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
        }
    }

    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            initializeOpenAiService();
        }
        return openAiService;
    }

    public ChatResponse processMessage(ChatRequest request) {
        String message = request.getMessage();
        String conversationId = request.getConversationId();
        
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
            logger.debug("Generated new conversation ID: {}", conversationId);
        }

        // Safety check
        SafetyResult safetyResult = performSafetyCheck(message);
        if (!safetyResult.isSafe()) {
            logger.warn("Safety check failed for conversation {}: {}", conversationId, safetyResult.getReason());
            ChatResponse response = new ChatResponse();
            response.setMessage("I'm sorry, but I can't process that request. " + safetyResult.getReason());
            response.setSafetyResult(safetyResult);
            response.setConversationId(conversationId);
            return response;
        }

        // Use sanitized message if available
        String processedMessage = safetyResult.getSanitizedMessage() != null ? 
                safetyResult.getSanitizedMessage() : message;

        // Detect intent
        String intent = detectIntent(processedMessage);
        logger.debug("Detected intent: {} for conversation: {}", intent, conversationId);
        
        // Get relevant phones based on intent
        List<Phone> relevantPhones = getRelevantPhones(processedMessage, intent);
        logger.debug("Found {} relevant phones for intent: {}", relevantPhones.size(), intent);
        
        // Generate response
        String aiResponse = generateResponse(processedMessage, intent, relevantPhones, conversationId);
        
        // Build recommendations if applicable
        List<PhoneRecommendation> recommendations = buildRecommendations(relevantPhones, intent, processedMessage);
        
        // Extract comparison phones if compare intent
        List<Phone> comparisonPhones = extractComparisonPhones(processedMessage, intent, relevantPhones);
        
        ChatResponse response = new ChatResponse();
        response.setMessage(aiResponse);
        response.setRecommendations(recommendations);
        response.setComparisonPhones(comparisonPhones);
        response.setSafetyResult(safetyResult);
        response.setConversationId(conversationId);
        response.setIntent(intent);
        
        return response;
    }

    private SafetyResult performSafetyCheck(String message) {
        SafetyResult result = new SafetyResult();
        
        // Basic safety checks
        String lowerMessage = message.toLowerCase();
        
        // Check for inappropriate content
        String[] inappropriateTerms = {"hack", "crack", "illegal", "scam"};
        for (String term : inappropriateTerms) {
            if (lowerMessage.contains(term)) {
                result.setSafe(false);
                result.setReason("The message contains content that cannot be processed.");
                return result;
            }
        }
        
        // Check message length (prevent abuse)
        if (message.length() > 1000) {
            result.setSafe(false);
            result.setReason("Message is too long. Please keep it under 1000 characters.");
            return result;
        }
        
        // Sanitize HTML/script tags if any
        String sanitized = message.replaceAll("<[^>]*>", "");
        if (!sanitized.equals(message)) {
            result.setSanitizedMessage(sanitized);
        }
        
        return result;
    }

    private String detectIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (COMPARE_PATTERN.matcher(lowerMessage).find()) {
            return "compare";
        } else if (RECOMMEND_PATTERN.matcher(lowerMessage).find()) {
            return "recommend";
        } else if (PRICE_PATTERN.matcher(lowerMessage).find() || lowerMessage.contains("price") || lowerMessage.contains("cost")) {
            return "search_by_price";
        } else if (BRAND_PATTERN.matcher(message).find() || lowerMessage.contains("brand")) {
            return "search_by_brand";
        } else if (FEATURE_PATTERN.matcher(lowerMessage).find() || lowerMessage.contains("feature")) {
            return "search_by_feature";
        } else if (lowerMessage.contains("all") || lowerMessage.contains("list") || lowerMessage.contains("show")) {
            return "list_all";
        } else {
            return "general";
        }
    }

    private List<Phone> getRelevantPhones(String message, String intent) {
        List<Phone> phones = new ArrayList<>();
        
        switch (intent) {
            case "list_all":
                phones = phoneCatalogService.getAllPhones();
                break;
                
            case "search_by_price":
                phones = extractAndSearchByPrice(message);
                break;
                
            case "search_by_brand":
                phones = extractAndSearchByBrand(message);
                break;
                
            case "search_by_feature":
                phones = extractAndSearchByFeature(message);
                break;
                
            case "recommend":
                phones = extractAndSearchForRecommendation(message);
                break;
                
            case "compare":
                phones = extractAndSearchForComparison(message);
                break;
                
            default:
                // General query - try to find phones by name first
                phones = phoneCatalogService.searchByName(message);
                if (phones.isEmpty()) {
                    // Fallback: return all phones for general queries
                    phones = phoneCatalogService.getAllPhones();
                }
                break;
        }
        
        // Limit results to top 10
        return phones.stream().limit(10).collect(Collectors.toList());
    }

    private List<Phone> extractAndSearchByPrice(String message) {
        String lowerMessage = message.toLowerCase();
        java.util.regex.Matcher matcher = PRICE_PATTERN.matcher(lowerMessage);
        if (matcher.find()) {
            try {
                String priceStr = matcher.group(1);
                Integer maxPrice = Integer.parseInt(priceStr);
                // If number is small (like "5" or "30"), assume it's in thousands
                if (maxPrice < 100) {
                    maxPrice = maxPrice * 1000;
                }
                return phoneCatalogService.searchByPriceRange(maxPrice);
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        return phoneCatalogService.getAllPhones();
    }

    private List<Phone> extractAndSearchByBrand(String message) {
        java.util.regex.Matcher matcher = BRAND_PATTERN.matcher(message);
        if (matcher.find()) {
            String brand = matcher.group(1);
            // Normalize brand names
            if (brand.equalsIgnoreCase("pixel")) {
                brand = "Google";
            }
            return phoneCatalogService.searchByBrand(brand);
        }
        return phoneCatalogService.getAllPhones();
    }

    private List<Phone> extractAndSearchByFeature(String message) {
        java.util.regex.Matcher matcher = FEATURE_PATTERN.matcher(message);
        if (matcher.find()) {
            String feature = matcher.group(1);
            return phoneCatalogService.searchByFeature(feature);
        }
        return phoneCatalogService.getAllPhones();
    }

    private List<Phone> extractAndSearchForRecommendation(String message) {
        List<Phone> phones = new ArrayList<>();
        
        // Try price first
        List<Phone> pricePhones = extractAndSearchByPrice(message);
        if (!pricePhones.isEmpty()) {
            phones.addAll(pricePhones);
        }
        
        // Try brand
        List<Phone> brandPhones = extractAndSearchByBrand(message);
        if (!brandPhones.isEmpty()) {
            phones.addAll(brandPhones);
        }
        
        // Try feature
        List<Phone> featurePhones = extractAndSearchByFeature(message);
        if (!featurePhones.isEmpty()) {
            phones.addAll(featurePhones);
        }
        
        // If nothing found, return all phones
        if (phones.isEmpty()) {
            phones = phoneCatalogService.getAllPhones();
        }
        
        // Remove duplicates
        return phones.stream()
                .distinct()
                .sorted(Comparator.comparing(Phone::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<Phone> extractAndSearchForComparison(String message) {
        List<Phone> phones = new ArrayList<>();
        
        // Extract phone names from message
        List<String> phoneNames = new ArrayList<>();
        String[] allPhones = {"OnePlus 12R", "Pixel 8a", "Samsung Galaxy A54", "Redmi Note 13 Pro", 
                              "Nothing Phone 2a", "Realme 12 Pro", "Vivo V29", "Motorola Edge 40"};
        
        for (String phoneName : allPhones) {
            if (message.toLowerCase().contains(phoneName.toLowerCase())) {
                phoneNames.add(phoneName);
            }
        }
        
        if (!phoneNames.isEmpty()) {
            phones = phoneCatalogService.findMultipleByName(phoneNames);
        } else {
            // Default comparison: get first 2-3 phones
            phones = phoneCatalogService.getAllPhones().stream()
                    .limit(3)
                    .collect(Collectors.toList());
        }
        
        return phones;
    }

    private List<PhoneRecommendation> buildRecommendations(List<Phone> phones, String intent, String message) {
        if (!"recommend".equals(intent) || phones.isEmpty()) {
            return null;
        }
        
        List<PhoneRecommendation> recommendations = new ArrayList<>();
        
        for (Phone phone : phones) {
            PhoneRecommendation rec = new PhoneRecommendation();
            rec.setPhone(phone);
            
            // Build rationale
            StringBuilder rationale = new StringBuilder();
            if (phone.getPrice() != null) {
                rationale.append("Priced at ₹").append(phone.getPrice());
            }
            if (phone.getCamera() != null) {
                if (rationale.length() > 0) rationale.append(", ");
                rationale.append("features a ").append(phone.getCamera()).append(" camera");
            }
            if (phone.getBattery() != null) {
                if (rationale.length() > 0) rationale.append(", ");
                rationale.append(phone.getBattery()).append(" battery");
            }
            if (phone.getFeatures() != null && !phone.getFeatures().isEmpty()) {
                if (rationale.length() > 0) rationale.append(", ");
                rationale.append("key features: ").append(String.join(", ", phone.getFeatures().subList(0, Math.min(3, phone.getFeatures().size()))));
            }
            
            rec.setRationale(rationale.toString());
            rec.setRelevanceScore(calculateRelevanceScore(phone, message));
            recommendations.add(rec);
        }
        
        // Sort by relevance score
        recommendations.sort((a, b) -> Double.compare(
                b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0));
        
        return recommendations;
    }

    private Double calculateRelevanceScore(Phone phone, String message) {
        double score = 0.5; // Base score
        
        String lowerMessage = message.toLowerCase();
        
        // Check if phone name is mentioned
        if (phone.getName() != null && lowerMessage.contains(phone.getName().toLowerCase())) {
            score += 0.3;
        }
        
        // Check if brand matches
        if (phone.getBrand() != null && lowerMessage.contains(phone.getBrand().toLowerCase())) {
            score += 0.2;
        }
        
        // Check feature matches
        if (phone.getFeatures() != null) {
            for (String feature : phone.getFeatures()) {
                if (lowerMessage.contains(feature.toLowerCase())) {
                    score += 0.1;
                }
            }
        }
        
        return Math.min(1.0, score);
    }

    private List<Phone> extractComparisonPhones(String message, String intent, List<Phone> relevantPhones) {
        if (!"compare".equals(intent)) {
            return null;
        }
        return relevantPhones.stream().limit(3).collect(Collectors.toList());
    }

    private String generateResponse(String message, String intent, List<Phone> relevantPhones, String conversationId) {
        // Build system prompt
        String systemPrompt = buildSystemPrompt(intent, relevantPhones);
        
        // Prepare messages for OpenAI
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        
        // Get conversation history thread-safely
        List<ChatMessage> history = conversationHistory.getOrDefault(conversationId, new ArrayList<>());
        
        // Add conversation history (last 5 messages)
        int historySize = Math.min(history.size(), 10); // Keep last 10 messages (5 pairs)
        for (int i = history.size() - historySize; i < history.size(); i++) {
            messages.add(history.get(i));
        }
        
        // Add current user message
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), message);
        messages.add(userMessage);
        
        // Generate response using OpenAI if API key is available
        String aiResponse;
        OpenAiService service = getOpenAiService();
        if (service != null) {
            try {
                ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                        .model(openaiModel)
                        .messages(messages)
                        .temperature(0.7)
                        .maxTokens(500)
                        .build();
                
                ChatMessage responseMessage = service.createChatCompletion(completionRequest)
                        .getChoices().get(0).getMessage();
                
                aiResponse = responseMessage.getContent();
                
                // Update conversation history thread-safely using compute()
                conversationHistory.compute(conversationId, (key, existingHistory) -> {
                    List<ChatMessage> updatedHistory = existingHistory != null 
                            ? new ArrayList<>(existingHistory) 
                            : new ArrayList<>();
                    updatedHistory.add(userMessage);
                    updatedHistory.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiResponse));
                    return updatedHistory;
                });
                
            } catch (Exception e) {
                // Fallback to rule-based response if OpenAI fails
                logger.warn("OpenAI API error, using fallback response: {}", e.getMessage(), e);
                aiResponse = generateFallbackResponse(message, intent, relevantPhones);
            }
        } else {
            // No API key - use fallback response
            aiResponse = generateFallbackResponse(message, intent, relevantPhones);
        }
        
        return aiResponse;
    }

    private String buildSystemPrompt(String intent, List<Phone> phones) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful phone shopping assistant. ");
        
        if (!phones.isEmpty()) {
            prompt.append("Here are some phones from our catalog:\n\n");
            for (Phone phone : phones) {
                prompt.append("- ").append(phone.getName()).append(" (");
                prompt.append(phone.getBrand()).append(")");
                if (phone.getPrice() != null) {
                    prompt.append(" - ₹").append(phone.getPrice());
                }
                if (phone.getCamera() != null) {
                    prompt.append(", Camera: ").append(phone.getCamera());
                }
                if (phone.getBattery() != null) {
                    prompt.append(", Battery: ").append(phone.getBattery());
                }
                if (phone.getFeatures() != null && !phone.getFeatures().isEmpty()) {
                    prompt.append(", Features: ").append(String.join(", ", phone.getFeatures()));
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("Provide helpful, concise, and friendly responses. ");
        prompt.append("Focus on helping customers find the right phone for their needs. ");
        prompt.append("Be conversational and natural. ");
        
        switch (intent) {
            case "compare":
                prompt.append("Compare the phones mentioned, highlighting key differences in price, features, camera, and battery.");
                break;
            case "recommend":
                prompt.append("Recommend phones based on the customer's requirements, explaining why each is a good fit.");
                break;
            case "search_by_price":
                prompt.append("Help the customer find phones within their budget, highlighting value for money.");
                break;
            default:
                prompt.append("Answer the customer's question about phones.");
                break;
        }
        
        return prompt.toString();
    }

    private String generateFallbackResponse(String message, String intent, List<Phone> phones) {
        StringBuilder response = new StringBuilder();
        String lowerMessage = message.toLowerCase();
        
        if (phones.isEmpty()) {
            return "I couldn't find any phones matching your request. Could you please provide more details?";
        }
        
        switch (intent) {
            case "compare":
                response.append("Here are some phones for comparison:\n\n");
                for (Phone phone : phones) {
                    response.append("**").append(phone.getName()).append("**\n");
                    if (phone.getPrice() != null) {
                        response.append("Price: ₹").append(phone.getPrice()).append("\n");
                    }
                    if (phone.getCamera() != null) {
                        response.append("Camera: ").append(phone.getCamera()).append("\n");
                    }
                    if (phone.getBattery() != null) {
                        response.append("Battery: ").append(phone.getBattery()).append("\n");
                    }
                    if (phone.getProcessor() != null) {
                        response.append("Processor: ").append(phone.getProcessor()).append("\n");
                    }
                    response.append("\n");
                }
                break;
                
            case "recommend":
                response.append("Based on your requirements, here are some recommendations:\n\n");
                for (int i = 0; i < Math.min(3, phones.size()); i++) {
                    Phone phone = phones.get(i);
                    response.append((i + 1)).append(". **").append(phone.getName()).append("**");
                    if (phone.getPrice() != null) {
                        response.append(" - ₹").append(phone.getPrice());
                    }
                    response.append("\n");
                    if (phone.getCamera() != null || phone.getBattery() != null) {
                        response.append("   ");
                        if (phone.getCamera() != null) {
                            response.append(phone.getCamera()).append(" camera, ");
                        }
                        if (phone.getBattery() != null) {
                            response.append(phone.getBattery()).append(" battery");
                        }
                        response.append("\n");
                    }
                    response.append("\n");
                }
                break;
                
            case "list_all":
                response.append("Here are all available phones:\n\n");
                for (Phone phone : phones) {
                    response.append("- ").append(phone.getName());
                    if (phone.getPrice() != null) {
                        response.append(" (₹").append(phone.getPrice()).append(")");
                    }
                    response.append("\n");
                }
                break;
                
            default:
                response.append("I found ").append(phones.size()).append(" phone(s) that might interest you:\n\n");
                for (Phone phone : phones) {
                    response.append("- **").append(phone.getName()).append("**");
                    if (phone.getPrice() != null) {
                        response.append(" - ₹").append(phone.getPrice());
                    }
                    response.append("\n");
                }
                response.append("\nWould you like more details about any of these phones?");
                break;
        }
        
        return response.toString();
    }
}

