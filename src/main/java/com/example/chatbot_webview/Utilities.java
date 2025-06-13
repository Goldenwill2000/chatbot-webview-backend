package com.example.chatbot_webview;

import java.util.Set;

public class Utilities {
    private static final Set<String> COUNTRY_CODES = Set.of(
            "1", "44", "60", "65", "91", "81", "49", "33", "61", "55"
    );

    // Public static utility method
    public static String extractCountryCode(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }

        // Clean input
        phoneNumber = phoneNumber.replaceAll("^\\+", "").replaceAll("\\D", "");

        for (int len = 1; len <= 3 && len <= phoneNumber.length(); len++) {
            String prefix = phoneNumber.substring(0, len);
            if (COUNTRY_CODES.contains(prefix)) {
                return prefix;
            }
        }

        return "";
    }
}
