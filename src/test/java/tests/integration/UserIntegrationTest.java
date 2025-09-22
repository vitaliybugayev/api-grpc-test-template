package tests.integration;

import base.BaseGrpcTest;
import client.grpc.UserServiceClient;
import com.example.grpc.userservice.UserServiceProto.*;
import io.qameta.allure.*;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import steps.UserApiSteps;
import utils.TestDataGenerator;

@Epic("Integration Testing")
@Feature("API and gRPC Integration")
public class UserIntegrationTest extends BaseGrpcTest {

    private UserServiceClient userServiceClient;
    private UserApiSteps userApiSteps;

    @BeforeClass
    public void setUp() {
        userServiceClient = new UserServiceClient();
        registerGrpcClient(userServiceClient.getBaseClient());
        
        userApiSteps = new UserApiSteps();
        
        LOG.info("Integration test setup completed");
    }

    @Test
    @Description("Create user via gRPC and verify via REST API")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Cross-protocol user management")
    public void createUserGrpcVerifyApiIntegrationTest() {
        // Step 1: Create user via gRPC
        final var name = TestDataGenerator.randomName();
        final var email = TestDataGenerator.randomEmail();
        final var age = TestDataGenerator.randomAge();

        var grpcRequest = 
                com.example.grpc.userservice.UserServiceProto.CreateUserRequest.newBuilder()
                .setName(name)
                .setEmail(email)
                .setAge(age)
                .build();

        String userId = null;
        try {
            var grpcResponse = userServiceClient.createUser(grpcRequest);
            userId = grpcResponse.getUser().getId();
            final var finalUserId = userId; // Make effectively final for lambda

            // Step 2: Verify user exists via REST API
            var apiResponse = userApiSteps.getUserById(userId);
            var apiUser = userApiSteps.extractUserFromResponse(apiResponse);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(grpcResponse.getSuccess()).isTrue();
                // Step 3: Compare data consistency
                softly.assertThat(apiUser.id()).isEqualTo(finalUserId);
                softly.assertThat(apiUser.name()).isEqualTo(name);
                softly.assertThat(apiUser.email()).isEqualTo(email);
                softly.assertThat(apiUser.age()).isEqualTo(age);
            });
        } finally {
            if (userId != null) {
                // Cleanup
                try {
                    var deleteRequest = DeleteUserRequest.newBuilder()
                            .setId(userId)
                            .build();
                    userServiceClient.deleteUser(deleteRequest);
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Test
    @Description("Create user via REST API and verify via gRPC")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Cross-protocol user management")
    public void createUserApiVerifyGrpcIntegrationTest() {
        // Step 1: Create user via REST API
        var apiRequest = new models.requests.CreateUserRequest(
                TestDataGenerator.randomName(),
                TestDataGenerator.randomEmail(), 
                TestDataGenerator.randomAge());
        
        var apiResponse = userApiSteps.createUser(apiRequest);
        var apiUser = userApiSteps.extractUserFromResponse(apiResponse);

        // Step 2: Verify user exists via gRPC
        var grpcRequest = GetUserRequest.newBuilder()
                .setId(apiUser.id())
                .build();

        var grpcResponse = userServiceClient.getUser(grpcRequest);

        SoftAssertions.assertSoftly(softly -> {
            // Step 3: Compare data consistency
            softly.assertThat(grpcResponse.getFound()).isTrue();
            softly.assertThat(grpcResponse.getUser().getId()).isEqualTo(apiUser.id());
            softly.assertThat(grpcResponse.getUser().getName()).isEqualTo(apiUser.name());
            softly.assertThat(grpcResponse.getUser().getEmail()).isEqualTo(apiUser.email());
            softly.assertThat(grpcResponse.getUser().getAge()).isEqualTo(apiUser.age());
        });

        // Cleanup
        userApiSteps.deleteUser(apiUser.id());
    }

    @Test
    @Description("Update user via gRPC and verify via REST API")
    @Severity(SeverityLevel.NORMAL)
    @Story("Cross-protocol user updates")
    public void updateUserGrpcVerifyApiIntegrationTest() {
        // Setup: Create user via API
        var createRequest = new models.requests.CreateUserRequest(
                TestDataGenerator.randomName(),
                TestDataGenerator.randomEmail(), 
                TestDataGenerator.randomAge());
        var createResponse = userApiSteps.createUser(createRequest);
        var createdUser = userApiSteps.extractUserFromResponse(createResponse);

        // Step 1: Update user via gRPC
        var newName = TestDataGenerator.randomName();
        var newEmail = TestDataGenerator.randomEmail();
        var newAge = TestDataGenerator.randomAge();

        var grpcUpdateRequest = UpdateUserRequest.newBuilder()
                .setId(createdUser.id())
                .setName(newName)
                .setEmail(newEmail)
                .setAge(newAge)
                .setStatus(UserStatus.ACTIVE)
                .build();

        var grpcUpdateResponse = userServiceClient.updateUser(grpcUpdateRequest);

        // Step 2: Verify update via REST API
        var apiGetResponse = userApiSteps.getUserById(createdUser.id());
        var updatedUser = userApiSteps.extractUserFromResponse(apiGetResponse);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(grpcUpdateResponse.getSuccess()).isTrue();
            // Step 3: Validate consistency
            softly.assertThat(updatedUser.name()).isEqualTo(newName);
            softly.assertThat(updatedUser.email()).isEqualTo(newEmail);
            softly.assertThat(updatedUser.age()).isEqualTo(newAge);
        });

        // Cleanup
        userApiSteps.deleteUser(createdUser.id());
    }

    @Test
    @Description("Test data consistency between API and gRPC for list operations")
    @Severity(SeverityLevel.NORMAL)
    @Story("List operations consistency")
    public void listUsersConsistencyIntegrationTest() {
        // Create test users via both APIs
        var apiRequest = new models.requests.CreateUserRequest(
                TestDataGenerator.randomName(),
                TestDataGenerator.randomEmail(), 
                TestDataGenerator.randomAge());
        var apiResponse = userApiSteps.createUser(apiRequest);
        var apiUser = userApiSteps.extractUserFromResponse(apiResponse);

        var grpcRequest =
                com.example.grpc.userservice.UserServiceProto.CreateUserRequest.newBuilder()
                        .setName(TestDataGenerator.randomName())
                        .setEmail(TestDataGenerator.randomEmail())
                        .setAge(TestDataGenerator.randomAge())
                        .build();

        var grpcResponse = userServiceClient.createUser(grpcRequest);

        // Collect created IDs into an array, then use streams
        var userIds = new String[]{apiUser.id(), grpcResponse.getUser().getId()};

        // List users via both APIs and compare
        var apiListResponse = userApiSteps.getUsersList(1, 100, "");
        var grpcListRequest = ListUsersRequest.newBuilder()
                .setPage(1)
                .setSize(100)
                .setFilter("")
                .build();
        var grpcListResponse = userServiceClient.listUsers(grpcListRequest);

        var apiTotalCount = apiListResponse.jsonPath().getInt("total_count");
        var grpcTotalCount = grpcListResponse.getTotalCount();

        // Build sets via streams for presence checks
        var apiIds = new java.util.HashSet<>(apiListResponse.jsonPath().getList("users.id", String.class));
        var grpcIds = grpcListResponse.getUsersList().stream().map(u -> u.getId()).collect(java.util.stream.Collectors.toSet());
        var createdIds = java.util.Arrays.stream(userIds).collect(java.util.stream.Collectors.toSet());

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(apiTotalCount).as("total_count: API vs gRPC").isEqualTo(grpcTotalCount);
            softly.assertThat(apiIds).as("API list contains created IDs").containsAll(createdIds);
            softly.assertThat(grpcIds).as("gRPC list contains created IDs").containsAll(createdIds);
        });

        // Cleanup via streams
        java.util.Arrays.stream(userIds).forEach(userApiSteps::deleteUser);
    }

    @Test
    @Description("Test error handling consistency between API and gRPC")
    @Severity(SeverityLevel.MINOR)
    @Story("Error handling consistency")
    public void errorHandlingConsistencyIntegrationTest() {
        var nonExistentId = TestDataGenerator.randomId();

        // Test 404/NOT_FOUND via REST API
        userApiSteps.verifyUserNotFound(nonExistentId);

        // Test NOT_FOUND via gRPC
        var grpcRequest = GetUserRequest.newBuilder()
                .setId(nonExistentId)
                .build();

        var grpcResponse = userServiceClient.getUser(grpcRequest);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(grpcResponse.getFound()).isFalse();
        });
    }
}
