package org.example.sandbox_backend.model;

import org.example.sandbox_backend.entities.UserInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming (PropertyNamingStrategy.SnakeCaseStrategy.class)
public class UserInfoDto extends UserInfo{

    private String firstName; // first_name

    private String lastName; //last_name

    private Long phoneNumber;

    private String email; // email


}
