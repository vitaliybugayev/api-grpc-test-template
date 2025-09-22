package models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import models.UserModel;

import java.util.List;

public record UserListResponse(
        @JsonProperty("users") List<UserModel> users,
        @JsonProperty("total_count") Integer totalCount,
        @JsonProperty("page") Integer page,
        @JsonProperty("size") Integer size
) {}
