package com.gitu.expense_tracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
public class CategoryService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Other");

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    public String categorize(String description) {

        if (description == null || description.isBlank()) {
            return "Other";
        }

        String prompt = "Classify this expense description into exactly ONE category word " +

                "from this list: Food, Travel, Shopping, Bills, Entertainment, Health, Other. " +
                "Respond with ONLY the category word, nothing else.\n\n" +
                "Expense: \"" + description + "\"";

        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return fallbackCategory(description);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));
        body.put("temperature", 0.2);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            if (response == null) {
                return fallbackCategory(description);
            }
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageObj = (Map<?, ?>) firstChoice.get("message");
            String category = messageObj.get("content").toString().trim();
            for (String allowed : ALLOWED_CATEGORIES) {
                if (allowed.equalsIgnoreCase(category)) return allowed;
            }
            return fallbackCategory(description);
        } catch (Exception e) {
            // A provider outage must not prevent a user from recording an expense.
            return fallbackCategory(description);
        }
    }

    private String fallbackCategory(String description) {
        String value = description.toLowerCase(Locale.ROOT);
        if (value.matches(".*\\b(swiggy|zomato|restaurant|lunch|dinner|breakfast|food|grocery|groceries)\\b.*")) {
            return "Food";
        }
        if (value.matches(".*\\b(flight|train|uber|ola|taxi|hotel|travel|bus|metro)\\b.*")) {
            return "Travel";
        }
        if (value.matches(".*\\b(amazon|flipkart|shopping|clothes|shoes|mall)\\b.*")) {
            return "Shopping";
        }
        if (value.matches(".*\\b(electricity|water|rent|recharge|bill|bills|internet)\\b.*")) {
            return "Bills";
        }
        if (value.matches(".*\\b(movie|cinema|netflix|concert|game|entertainment)\\b.*")) {
            return "Entertainment";
        }
        if (value.matches(".*\\b(hospital|doctor|medicine|pharmacy|health)\\b.*")) {
            return "Health";
        }
        return "Other";
    }
}
