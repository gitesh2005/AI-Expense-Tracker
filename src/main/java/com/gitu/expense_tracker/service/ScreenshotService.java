package com.gitu.expense_tracker.service;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScreenshotService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    public Map<String, String> extractExpenseFromImage(MultipartFile imageFile) throws IOException {

        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
        String mimeType = imageFile.getContentType();

        String prompt = "This is a screenshot of a payment confirmation or invoice (UPI payment, Amazon order, or similar). " +
                "Extract ONLY two things: the item/merchant name (keep it short, a few words) and the FINAL total amount paid " +
                "(ignore any tax breakdowns, just give the final total). " +
                "Respond ONLY in this exact JSON format, nothing else, no explanation: " +
                "{\"description\": \"<short item or merchant name>\", \"amount\": <final total as a number>}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        Map<String, Object> imageUrlObj = new HashMap<>();
        imageUrlObj.put("url", "data:" + mimeType + ";base64," + base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrlObj);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textPart, imagePart));

        Map<String, Object> body = new HashMap<>();
        body.put("model", "qwen/qwen3.6-27b");
        body.put("messages", List.of(message));
        body.put("temperature", 0.2);
        body.put("max_completion_tokens", 500);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(apiUrl, request, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AI could not read this image. Try a clearer screenshot showing the amount and merchant name.");
        }

        try {
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageObj = (Map<?, ?>) firstChoice.get("message");
            String content = messageObj.get("content").toString();

            System.out.println("=== RAW AI RESPONSE ===");
            System.out.println(content);
            System.out.println("=======================");

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(content, Map.class);

            Map<String, String> result = new HashMap<>();
            result.put("description", parsed.get("description").toString());
            result.put("amount", parsed.get("amount").toString());
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not extract expense details clearly. Try a different screenshot.");
        }
    }
}