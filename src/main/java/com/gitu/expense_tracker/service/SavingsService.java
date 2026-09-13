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

@Service
public class SavingsService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    public String getSavingsSuggestion(Map<String, Double> categoryTotals) {

        StringBuilder dataText = new StringBuilder();

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            dataText.append("- ").append(entry.getKey()).append(": ₹").append(entry.getValue()).append("\n");
        }

        String prompt = "Here is a breakdown of someone's monthly spending by category:\n\n" +
                dataText +
                "\nGive a short, practical 3-4 sentence suggestion on which category they should cut back on " +
                "and one actionable tip. Be direct and specific, not generic.";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));
        body.put("temperature", 0.4);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageObj = (Map<?, ?>) firstChoice.get("message");
            return messageObj.get("content").toString().trim();
        } catch (Exception e) {

            return "Unable to generate suggestion right now.";
        }
    }
}