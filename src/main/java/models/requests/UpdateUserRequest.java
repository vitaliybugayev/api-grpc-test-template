package models.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import models.UserModel.UserStatus;

@Builder
public record UpdateUserRequest(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("age") Integer age,
        @JsonProperty("status") UserStatus status
) {}
