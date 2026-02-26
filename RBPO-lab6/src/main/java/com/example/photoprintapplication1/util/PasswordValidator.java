package com.example.photoprintapplication1.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    private static final Pattern UPPER_CASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).find();
        boolean hasUpperCase = UPPER_CASE_PATTERN.matcher(password).find();
        boolean hasLowerCase = LOWER_CASE_PATTERN.matcher(password).find();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).find();

        return hasSpecialChar && hasUpperCase && hasLowerCase && hasDigit;
    }

    public String getPasswordRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain at least one uppercase letter, " +
                "one lowercase letter, one digit, and one special character.";
    }
}