package com.example.sandbox_backend.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final String USERNAME_PATTERN = "^[\\w.@+-]{5,}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d])[A-Za-z\\d[^\\s]]{6,20}$";


    private static final Pattern usernameRegex = Pattern.compile(USERNAME_PATTERN);
    private static final Pattern passwordRegex = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isJson(String json) {
        return json.startsWith("{") && json.endsWith("}");
    }
}