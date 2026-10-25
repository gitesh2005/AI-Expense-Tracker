package com.gitu.expense_tracker;

import com.gitu.expense_tracker.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryServiceFallbackTest {

    @Test
    void categorizesLocallyWhenProviderIsNotConfigured() throws Exception {
        CategoryService service = new CategoryService();
        set(service, "apiUrl", "");
        set(service, "apiKey", "");

        assertEquals("Food", service.categorize("Swiggy lunch"));
        assertEquals("Other", service.categorize("miscellaneous payment"));
    }

    @Test
    void categorizesLocallyWhenProviderCallFails() throws Exception {
        CategoryService service = new CategoryService();
        set(service, "apiUrl", "https://example.invalid/chat");
        set(service, "apiKey", "test-key");
        set(service, "restTemplate", new RestTemplate() {
            @Override
            public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
                throw new IllegalStateException("provider unavailable");
            }
        });

        assertEquals("Travel", service.categorize("Uber airport ride"));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
