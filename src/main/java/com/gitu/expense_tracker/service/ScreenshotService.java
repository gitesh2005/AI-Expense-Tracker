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
import java.util.Set;

@Service
public class ScreenshotService {

    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.screenshot-model:qwen/qwen3.6-27b}")
    private String screenshotModel;

    public Map<String, String> extractExpenseFromImage(MultipartFile imageFile) throws IOException {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image is required and must not be empty");
        }
        if (imageFile.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be non-empty and no larger than 5 MB");
        }
        String mimeType = imageFile.getContentType();
        if (mimeType != null) {
            mimeType = mimeType.split(";", 2)[0].trim().toLowerCase();
        }
        if (mimeType == null || !Set.of("image/png", "image/jpeg", "image/webp").contains(mimeType)) {
            throw new IllegalArgumentException("Only PNG, JPEG, and WebP images are supported");
        }
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Screenshot processing is not configured");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());

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
        body.put("model", screenshotModel);
        body.put("messages", List.of(message));
        body.put("temperature", 0.2);
        body.put("max_completion_tokens", 500);
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(apiUrl, request, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("AI could not read this image. Try a clearer screenshot showing the amount and merchant name.");
        }

        try {
            if (response == null) {
                throw new IllegalArgumentException("Empty AI response");
            }
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageObj = (Map<?, ?>) firstChoice.get("message");
            String content = messageObj.get("content").toString();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(content, Map.class);

            Map<String, String> result = new HashMap<>();
            Object description = parsed.get("description");
            Object amount = parsed.get("amount");
            if (description == null || amount == null) {
                throw new IllegalArgumentException("AI response is missing expense fields");
            }
            result.put("description", description.toString());
            result.put("amount", amount.toString());
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Could not extract expense details clearly. Try a different screenshot.");
        }
    }
}
