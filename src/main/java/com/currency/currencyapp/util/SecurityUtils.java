package com.currency.currencyapp.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for security-related operations.
 */
public class SecurityUtils {

    /**
     * Get the current authenticated user's ID from JWT token.
     *
     * @return the user ID or null if not authenticated
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // Handle JWT tokens (production)
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
            // Handle mock users (tests)
            else if (authentication.getName() != null && !authentication.getName().equals("anonymousUser")) {
                return authentication.getName();
            }
        }
        return null;
    }
}
