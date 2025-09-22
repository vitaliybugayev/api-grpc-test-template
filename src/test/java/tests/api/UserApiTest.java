package tests.api;

import base.BaseTest;
import io.qameta.allure.*;
import models.UserModel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import steps.UserApiSteps;
import utils.TestDataGenerator;

@Epic("API Testing")
@Feature("User API")
@Story("User CRUD via REST API")
public class UserApiTest extends BaseTest {

    private UserApiSteps userApiSteps;
    private String createdUserId;

    @BeforeClass
    public void setUp() {
        userApiSteps = new UserApiSteps();
    }

    @Test(priority = 1)
    @Description("Create a new user via API")
    @Severity(SeverityLevel.CRITICAL)
    public void createUserApiTest() {
        var request = new models.requests.CreateUserRequest(
                TestDataGenerator.randomName(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomAge());
        
        var response = userApiSteps.createUser(request);
        var createdUser = userApiSteps.extractUserFromResponse(response);
        
        // Store for cleanup and further tests
        this.createdUserId = createdUser.id();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(createdUser.name()).isEqualTo(request.name());
            softly.assertThat(createdUser.email()).isEqualTo(request.email());
            softly.assertThat(createdUser.age()).isEqualTo(request.age());
            softly.assertThat(createdUser.status()).isEqualTo(UserModel.UserStatus.ACTIVE);
            softly.assertThat(createdUser.id()).isNotNull();
        });
    }

    @Test(priority = 2, dependsOnMethods = "createUserApiTest")
    @Description("Get user by ID")
    @Severity(SeverityLevel.CRITICAL)
    public void getUserByIdApiTest() {
        var response = userApiSteps.getUserById(createdUserId);
        var user = userApiSteps.extractUserFromResponse(response);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(user.id()).isEqualTo(createdUserId);
            softly.assertThat(user.name()).isNotNull();
            softly.assertThat(user.email()).isNotNull();
            softly.assertThat(user.age()).isGreaterThan(0);
        });
    }

    @Test(priority = 3, dependsOnMethods = "createUserApiTest")
    @Description("Update user information")
    @Severity(SeverityLevel.NORMAL)
    public void updateUserApiTest() {
        var updateRequest = new models.requests.UpdateUserRequest(
                TestDataGenerator.randomName(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomAge(),
                models.UserModel.UserStatus.ACTIVE);
        
        var response = userApiSteps.updateUser(createdUserId, updateRequest);
        var updatedUser = userApiSteps.extractUserFromResponse(response);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(updatedUser.id()).isEqualTo(createdUserId);
            softly.assertThat(updatedUser.name()).isEqualTo(updateRequest.name());
            softly.assertThat(updatedUser.email()).isEqualTo(updateRequest.email());
            softly.assertThat(updatedUser.age()).isEqualTo(updateRequest.age());
            softly.assertThat(updatedUser.status()).isEqualTo(updateRequest.status());
        });
    }

    @Test(priority = 4)
    @Description("Get users list with pagination")
    @Severity(SeverityLevel.NORMAL)
    public void getUsersListApiTest() {
        var response = userApiSteps.getUsersList(1, 10, "");
        var userList = userApiSteps.extractUserListFromResponse(response);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(userList.users()).isNotNull();
            softly.assertThat(userList.page()).isEqualTo(1);
            softly.assertThat(userList.size()).isEqualTo(10);
            softly.assertThat(userList.totalCount()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test(priority = 5)
    @Description("Create user with invalid data should return validation error")
    @Severity(SeverityLevel.NORMAL)
    @Issue("API-001")
    public void createUserValidationErrorApiTest() {
        var invalidRequest = new models.requests.CreateUserRequest(
                "", // Invalid empty name
                "invalid-email", // Invalid email format
                -5); // Invalid negative age

        var resp = userApiSteps.createUserRaw(invalidRequest);
        org.assertj.core.api.SoftAssertions.assertSoftly(s -> {
            s.assertThat(resp.statusCode()).isEqualTo(400);
            s.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
            s.assertThat(resp.jsonPath().getString("error")).isNotBlank();
        });
    }

    @Test(priority = 6)
    @Description("Get non-existent user should return 404")
    @Severity(SeverityLevel.NORMAL)
    public void getUserNotFoundApiTest() {
        var nonExistentId = TestDataGenerator.randomId();

        var resp = userApiSteps.getUserByIdRaw(nonExistentId);
        org.assertj.core.api.SoftAssertions.assertSoftly(s -> {
            s.assertThat(resp.statusCode()).isEqualTo(404);
            s.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
            s.assertThat(resp.jsonPath().getString("error")).isNotBlank();
        });
    }

    @Test(priority = 7, dependsOnMethods = {"createUserApiTest", "getUserByIdApiTest", "updateUserApiTest"})
    @Description("Delete user")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteUserApiTest() {
        userApiSteps.deleteUser(createdUserId);
        
        // Verify user is deleted by trying to get it
        userApiSteps.verifyUserNotFound(createdUserId);
    }

    @Test
    @Description("Create user with null values should return validation error")
    @Severity(SeverityLevel.NORMAL)
    public void createUserWithNullValuesApiTest() {
        var nullRequest = new models.requests.CreateUserRequest(
                null,
                null,
                null);

        var resp = userApiSteps.createUserRaw(nullRequest);
        org.assertj.core.api.SoftAssertions.assertSoftly(s -> {
            s.assertThat(resp.statusCode()).isEqualTo(400);
            s.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
            s.assertThat(resp.jsonPath().getString("error")).isNotBlank();
        });
    }
}
