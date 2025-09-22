package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record UserModel(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("age") Integer age,
        @JsonProperty("status") @Builder.Default UserStatus status
) {
    public UserModel {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, BANNED
    }
}