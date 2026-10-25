package com.gitu.expense_tracker;

import com.gitu.expense_tracker.security.JwtService;
import com.gitu.expense_tracker.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void roundTripsAnAuthenticatedUser() {
        String secret = UUID.randomUUID().toString() + UUID.randomUUID();
        JwtService jwtService = new JwtService(secret, 3600);
        UserPrincipal expected = new UserPrincipal("user-123", "person@example.com");

        UserPrincipal actual = jwtService.parsePrincipal(jwtService.generateToken(expected));

        assertEquals(expected, actual);
    }

    @Test
    void refusesASecretThatIsTooShort() {
        JwtService jwtService = new JwtService("short", 3600);

        assertThrows(IllegalStateException.class,
                () -> jwtService.generateToken(new UserPrincipal("user-123", "person@example.com")));
    }
}
