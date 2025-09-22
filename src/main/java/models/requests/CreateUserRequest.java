package models.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("age") Integer age
) {}
