package steps;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import models.UserModel;
import models.requests.CreateUserRequest;
import models.requests.UpdateUserRequest;
import models.responses.UserListResponse;
import utils.Endpoints;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class UserApiSteps extends BaseApiSteps {

    @Step("Create user via API")
    public Response createUser(CreateUserRequest request) {
        return given()
                .body(request)
                .when()
                .post(Endpoints.USERS)
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.name", equalTo(request.name()))
                .body("data.email", equalTo(request.email()))
                .body("data.age", equalTo(request.age()))
                .extract()
                .response();
    }

    @Step("Create user via API (raw)")
    public Response createUserRaw(CreateUserRequest request) {
        return doPost(Endpoints.USERS, request);
    }

    @Step("Get user by ID: {userId}")
    public Response getUserById(String userId) {
        return given()
                .pathParam("id", userId)
                .when()
                .get(Endpoints.USER_BY_ID)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(userId))
                .body(matchesJsonSchemaInClasspath("schemas/user-get-response.json"))
                .extract()
                .response();
    }

    @Step("Get user by ID (raw): {userId}")
    public Response getUserByIdRaw(String userId) {
        return doGet(Endpoints.USER_BY_ID, Map.of("id", userId), null);
    }

    @Step("Update user by ID: {userId}")
    public Response updateUser(String userId, UpdateUserRequest request) {
        return given()
                .pathParam("id", userId)
                .body(request)
                .when()
                .put(Endpoints.USER_BY_ID)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(userId))
                .extract()
                .response();
    }

    @Step("Update user by ID (raw): {userId}")
    public Response updateUserRaw(String userId, UpdateUserRequest request) {
        return doPut(Endpoints.USER_BY_ID, Map.of("id", userId), request);
    }

    @Step("Delete user by ID: {userId}")
    public Response deleteUser(String userId) {
        return given()
                .pathParam("id", userId)
                .when()
                .delete(Endpoints.USER_BY_ID)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .extract()
                .response();
    }

    @Step("Delete user by ID (raw): {userId}")
    public Response deleteUserRaw(String userId) {
        return doDelete(Endpoints.USER_BY_ID, Map.of("id", userId));
    }

    @Step("Get users list with page: {page}, size: {size}")
    public Response getUsersList(int page, int size, String filter) {
        return given()
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("filter", filter)
                .when()
                .get(Endpoints.USERS)
                .then()
                .statusCode(200)
                .body("users", notNullValue())
                .body("total_count", greaterThanOrEqualTo(0))
                .body("page", equalTo(page))
                .body("size", equalTo(size))
                .extract()
                .response();
    }

    @Step("Get users list (raw) with page: {page}, size: {size}")
    public Response getUsersListRaw(int page, int size, String filter) {
        return doGet(Endpoints.USERS, null, Map.of(
                "page", page,
                "size", size,
                "filter", filter
        ));
    }

    @Step("Verify user not found for ID: {userId}")
    public Response verifyUserNotFound(String userId) {
        return given()
                .pathParam("id", userId)
                .when()
                .get(Endpoints.USER_BY_ID)
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("error", notNullValue())
                .extract()
                .response();
    }

    @Step("Create user and expect validation error")
    public Response createUserWithValidationError(CreateUserRequest request) {
        return given()
                .body(request)
                .when()
                .post(Endpoints.USERS)
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("error", notNullValue())
                .extract()
                .response();
    }

    @Step("Extract user from API response")
    public UserModel extractUserFromResponse(Response response) {
        return response.jsonPath().getObject("data", UserModel.class);
    }

    @Step("Extract user list from API response")
    public UserListResponse extractUserListFromResponse(Response response) {
        return response.as(UserListResponse.class);
    }
}
