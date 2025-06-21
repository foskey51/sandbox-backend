package org.example.sandbox_backend.utils;

import org.example.sandbox_backend.model.UserInfoDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

public class ValidationUtil {

    public final UserInfoDto userInfoDto;

    private static final String USERNAME_PATTERN = "^[\\w.@+-]{5,}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d])[A-Za-z\\d[^\\s]]{6,20}$";

    public ValidationUtil(UserInfoDto userInfoDto) {
        this.userInfoDto = userInfoDto;
    }

    private static final Pattern usernameRegex = Pattern.compile(USERNAME_PATTERN);
    private static final Pattern passwordRegex = Pattern.compile(PASSWORD_PATTERN);

    public static boolean validateUserAttributes(UserInfoDto userInfoDto) {
        if(userInfoDto.getUsername() == null || userInfoDto.getUsername().isEmpty() || userInfoDto.getPassword() == null || userInfoDto.getPassword().isEmpty()) {
            return false;
        }

        return usernameRegex.matcher(userInfoDto.getUsername()).matches() && passwordRegex.matcher(userInfoDto.getPassword()).matches();
    }

    public static boolean isJson(String json) {
        return json.startsWith("{") && json.endsWith("}");
    }
}
