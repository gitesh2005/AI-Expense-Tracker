package com.gitu.expense_tracker;

import com.gitu.expense_tracker.service.ScreenshotService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenshotServiceValidationTest {

    @Test
    void rejectsMissingAndUnsupportedUploadsBeforeCallingProvider() {
        ScreenshotService service = new ScreenshotService();

        assertThrows(IllegalArgumentException.class, () -> service.extractExpenseFromImage(null));
        MockMultipartFile text = new MockMultipartFile("image", "receipt.txt", "text/plain", "receipt".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.extractExpenseFromImage(text));
    }
}
