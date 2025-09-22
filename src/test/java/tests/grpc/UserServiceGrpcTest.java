package tests.grpc;

import base.BaseGrpcTest;
import client.grpc.UserServiceClient;
import com.example.grpc.userservice.UserServiceProto.*;
import io.qameta.allure.*;
import io.grpc.StatusRuntimeException;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utils.TestDataGenerator;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Epic("gRPC Testing")
@Feature("User gRPC Service")
@Story("User lifecycle and streaming via gRPC")
public class UserServiceGrpcTest extends BaseGrpcTest {

    private UserServiceClient userServiceClient;
    private List<String> createdUserIds = new ArrayList<>();

    @BeforeClass
    public void setUp() {
        // Initialize gRPC client (uses ENV or properties via BaseGrpcClient)
        userServiceClient = new UserServiceClient();
        registerGrpcClient(userServiceClient.getBaseClient());
        
        GRPC_LOG.info("UserServiceGrpcTest setup completed");
    }

    @AfterClass
    public void tearDown() {
        if (userServiceClient != null) {
            userServiceClient.shutdown();
        }
    }

    @Test(priority = 1)
    @Description("Create multiple users via gRPC stream")
    @Severity(SeverityLevel.CRITICAL)
    public void createUsersStreamGrpcTest() throws InterruptedException {
        List<CreateUserRequest> requests = IntStream.range(0, 5)
                .mapToObj(i -> CreateUserRequest.newBuilder()
                        .setName(TestDataGenerator.randomName())
                        .setEmail(TestDataGenerator.randomEmail())
                        .setAge(TestDataGenerator.randomAge())
                        .build())
                .collect(Collectors.toList());

        CreateUsersStreamResponse response = userServiceClient.createUsersStream(requests);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getTotalProcessed()).isEqualTo(5);
            softly.assertThat(response.getSuccessfulCount()).isEqualTo(5);
            softly.assertThat(response.getFailedCount()).isEqualTo(0);
            softly.assertThat(response.getResponsesList()).hasSize(5);
            
            for (int i = 0; i < response.getResponsesList().size(); i++) {
                CreateUserResponse userResponse = response.getResponsesList().get(i);
                CreateUserRequest originalRequest = requests.get(i);
                
                softly.assertThat(userResponse.getSuccess()).isTrue();
                softly.assertThat(userResponse.getUser().getName()).isEqualTo(originalRequest.getName());
                softly.assertThat(userResponse.getUser().getEmail()).isEqualTo(originalRequest.getEmail());
                softly.assertThat(userResponse.getUser().getAge()).isEqualTo(originalRequest.getAge());
                softly.assertThat(userResponse.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
                softly.assertThat(userResponse.getUser().getId()).isNotEmpty();
                
                createdUserIds.add(userResponse.getUser().getId());
            }
        });
    }

    @Test(priority = 2, dependsOnMethods = "createUsersStreamGrpcTest")
    @Description("Get multiple users by ID via gRPC stream")
    @Severity(SeverityLevel.CRITICAL)
    public void getUsersStreamGrpcTest() throws InterruptedException {
        List<GetUserRequest> requests = createdUserIds.stream()
                .map(id -> GetUserRequest.newBuilder().setId(id).build())
                .collect(Collectors.toList());

        List<GetUserResponse> responses = userServiceClient.getUsersStream(requests);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responses).hasSize(createdUserIds.size());
            
            for (int i = 0; i < responses.size(); i++) {
                GetUserResponse response = responses.get(i);
                String expectedId = createdUserIds.get(i);
                
                softly.assertThat(response.getFound()).isTrue();
                softly.assertThat(response.getUser().getId()).isEqualTo(expectedId);
                softly.assertThat(response.getUser().getName()).isNotEmpty();
                softly.assertThat(response.getUser().getEmail()).isNotEmpty();
                softly.assertThat(response.getUser().getAge()).isGreaterThan(0);
            }
        });
    }

    @Test(priority = 3, dependsOnMethods = "getUsersStreamGrpcTest")
    @Description("Update multiple users via gRPC stream")
    @Severity(SeverityLevel.NORMAL)
    public void updateUsersStreamGrpcTest() throws InterruptedException {
        List<UpdateUserRequest> requests = createdUserIds.stream()
                .map(id -> UpdateUserRequest.newBuilder()
                        .setId(id)
                        .setName(TestDataGenerator.randomName())
                        .setEmail(TestDataGenerator.randomEmail())
                        .setAge(TestDataGenerator.randomAge())
                        .setStatus(UserStatus.ACTIVE)
                        .build())
                .collect(Collectors.toList());

        List<UpdateUserResponse> responses = userServiceClient.updateUsersStream(requests);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(responses).hasSize(createdUserIds.size());
            
            for (int i = 0; i < responses.size(); i++) {
                UpdateUserResponse response = responses.get(i);
                UpdateUserRequest originalRequest = requests.get(i);
                
                softly.assertThat(response.getSuccess()).isTrue();
                softly.assertThat(response.getUser().getId()).isEqualTo(originalRequest.getId());
                softly.assertThat(response.getUser().getName()).isEqualTo(originalRequest.getName());
                softly.assertThat(response.getUser().getEmail()).isEqualTo(originalRequest.getEmail());
                softly.assertThat(response.getUser().getAge()).isEqualTo(originalRequest.getAge());
            }
        });
    }

    @Test(priority = 4)
    @Description("List users with pagination via gRPC")
    @Severity(SeverityLevel.NORMAL)
    public void listUsersGrpcTest() {
        var request = ListUsersRequest.newBuilder()
                .setPage(1)
                .setSize(10)
                .setFilter("")
                .build();

        var response = userServiceClient.listUsers(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getUsersList()).isNotNull();
            softly.assertThat(response.getPage()).isEqualTo(1);
            softly.assertThat(response.getSize()).isEqualTo(10);
            softly.assertThat(response.getTotalCount()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test(priority = 5)
    @Description("Get non-existent user should return not found")
    @Severity(SeverityLevel.NORMAL)
    @Issue("GRPC-001")
    public void getUserNotFoundGrpcTest() {
        var nonExistentId = TestDataGenerator.randomId();
        
        var request = GetUserRequest.newBuilder()
                .setId(nonExistentId)
                .build();

        var response = userServiceClient.getUser(request);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getFound()).isFalse();
        });
    }

    @Test(priority = 6, dataProvider = "invalidCreateUserRequests")
    @Description("Create user with invalid data should return error")
    @Severity(SeverityLevel.NORMAL)
    public void createUserValidationErrorGrpcTest(CreateUserRequest invalidRequest) {
        SoftAssertions.assertSoftly(softly -> {
            try {
                var response = userServiceClient.createUser(invalidRequest);
                // If no exception, check the response indicates failure
                softly.assertThat(response.getSuccess()).as("expected validation failure").isFalse();
                softly.assertThat(response.getErrorMessage()).as("expected error message").isNotEmpty();
            } catch (StatusRuntimeException e) {
                // Expected for validation errors
                softly.assertThat(java.util.Arrays.asList(
                                io.grpc.Status.Code.INVALID_ARGUMENT,
                                io.grpc.Status.Code.FAILED_PRECONDITION
                        ))
                        .as("expected INVALID_ARGUMENT or FAILED_PRECONDITION")
                        .contains(e.getStatus().getCode());
            }
        });
    }

    @org.testng.annotations.DataProvider(name = "invalidCreateUserRequests")
    public Object[][] invalidCreateUserRequests() {
        return new Object[][]{
                { CreateUserRequest.newBuilder().setName("").setEmail("valid@example.com").setAge(25).build() },
                { CreateUserRequest.newBuilder().setName("Valid Name").setEmail("not-an-email").setAge(25).build() },
                { CreateUserRequest.newBuilder().setName("Valid Name").setEmail("valid@example.com").setAge(-1).build() }
        };
    }

    @Test(priority = 7, dependsOnMethods = {"createUsersStreamGrpcTest", "getUsersStreamGrpcTest", "updateUsersStreamGrpcTest"})
    @Description("Delete multiple users via gRPC stream")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteUsersStreamGrpcTest() throws InterruptedException {
        List<DeleteUserRequest> requests = createdUserIds.stream()
                .map(id -> DeleteUserRequest.newBuilder().setId(id).build())
                .collect(Collectors.toList());

        DeleteUsersStreamResponse response = userServiceClient.deleteUsersStream(requests);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getTotalProcessed()).isEqualTo(createdUserIds.size());
            softly.assertThat(response.getSuccessfulCount()).isEqualTo(createdUserIds.size());
            softly.assertThat(response.getFailedCount()).isEqualTo(0);
            softly.assertThat(response.getResponsesList()).hasSize(createdUserIds.size());
            
            for (DeleteUserResponse deleteResponse : response.getResponsesList()) {
                softly.assertThat(deleteResponse.getSuccess()).isTrue();
            }
            
            // Verify all users are deleted by trying to get them
            for (String userId : createdUserIds) {
                var getRequest = GetUserRequest.newBuilder()
                        .setId(userId)
                        .build();
                var getResponse = userServiceClient.getUser(getRequest);
                softly.assertThat(getResponse.getFound()).isFalse();
            }
        });
    }

    @Test(priority = 8)
    @Description("Test bulk user creation with mixed valid and invalid data via stream")
    @Severity(SeverityLevel.NORMAL)
    public void bulkCreateUsersWithValidationStreamGrpcTest() throws InterruptedException {
        List<CreateUserRequest> requests = new ArrayList<>();
        
        // Add valid requests
        for (int i = 0; i < 3; i++) {
            requests.add(CreateUserRequest.newBuilder()
                    .setName(TestDataGenerator.randomName())
                    .setEmail(TestDataGenerator.randomEmail())
                    .setAge(TestDataGenerator.randomAge())
                    .build());
        }
        
        // Add invalid requests
        requests.add(CreateUserRequest.newBuilder()
                .setName("")
                .setEmail("valid@example.com")
                .setAge(25)
                .build());
        requests.add(CreateUserRequest.newBuilder()
                .setName("Valid Name")
                .setEmail("invalid-email")
                .setAge(25)
                .build());
        
        try {
            CreateUsersStreamResponse response = userServiceClient.createUsersStream(requests);
            
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getTotalProcessed()).isEqualTo(5);
                softly.assertThat(response.getSuccessfulCount()).isEqualTo(3);
                softly.assertThat(response.getFailedCount()).isEqualTo(2);
                
                // Cleanup successful creations
                List<String> successfulIds = response.getResponsesList().stream()
                        .filter(CreateUserResponse::getSuccess)
                        .map(r -> r.getUser().getId())
                        .collect(Collectors.toList());
                
                for (String id : successfulIds) {
                    try {
                        userServiceClient.deleteUser(DeleteUserRequest.newBuilder().setId(id).build());
                    } catch (Exception ignore) {}
                }
            });
        } catch (Exception e) {
            // Expected for mixed valid/invalid data
            GRPC_LOG.info("Bulk creation with validation errors handled: " + e.getMessage());
        }
    }

    @Test(priority = 9)
    @Description("Test large array processing via stream")
    @Severity(SeverityLevel.NORMAL)
    public void largeArrayStreamProcessingGrpcTest() throws InterruptedException {
        int arraySize = 50;
        List<CreateUserRequest> requests = IntStream.range(0, arraySize)
                .mapToObj(i -> CreateUserRequest.newBuilder()
                        .setName("User" + i + "_" + TestDataGenerator.randomName())
                        .setEmail("user" + i + "_" + TestDataGenerator.randomEmail())
                        .setAge(20 + (i % 50))
                        .build())
                .collect(Collectors.toList());

        List<String> createdIds = new ArrayList<>();
        
        try {
            CreateUsersStreamResponse response = userServiceClient.createUsersStream(requests);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getTotalProcessed()).isEqualTo(arraySize);
                softly.assertThat(response.getSuccessfulCount()).isEqualTo(arraySize);
                softly.assertThat(response.getFailedCount()).isEqualTo(0);
                
                // Collect IDs for verification
                for (CreateUserResponse userResponse : response.getResponsesList()) {
                    if (userResponse.getSuccess()) {
                        createdIds.add(userResponse.getUser().getId());
                    }
                }
                
                softly.assertThat(createdIds).hasSize(arraySize);
            });
            
            // Verify all users can be retrieved via stream
            List<GetUserRequest> getRequests = createdIds.stream()
                    .map(id -> GetUserRequest.newBuilder().setId(id).build())
                    .collect(Collectors.toList());
            
            List<GetUserResponse> getResponses = userServiceClient.getUsersStream(getRequests);
            
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getResponses).hasSize(arraySize);
                for (GetUserResponse getResponse : getResponses) {
                    softly.assertThat(getResponse.getFound()).isTrue();
                }
            });
            
        } finally {
            // Cleanup all created users
            if (!createdIds.isEmpty()) {
                try {
                    List<DeleteUserRequest> deleteRequests = createdIds.stream()
                            .map(id -> DeleteUserRequest.newBuilder().setId(id).build())
                            .collect(Collectors.toList());
                    userServiceClient.deleteUsersStream(deleteRequests);
                } catch (Exception e) {
                    GRPC_LOG.warn("Cleanup failed: " + e.getMessage());
                }
            }
        }
    }

    @Test
    @Description("Test concurrent gRPC calls")
    @Severity(SeverityLevel.MINOR)
    public void concurrentGrpcCallsTest() {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(3);
        var userIds = new java.util.ArrayList<String>();
        var futures = new java.util.ArrayList<java.util.concurrent.Future<CreateUserResponse>>();

        try {
            for (int i = 0; i < 3; i++) {
                futures.add(pool.submit(() -> {
                    var request = CreateUserRequest.newBuilder()
                            .setName(TestDataGenerator.randomName())
                            .setEmail(TestDataGenerator.randomEmail())
                            .setAge(TestDataGenerator.randomAge())
                            .build();
                    return userServiceClient.createUser(request);
                }));
            }

            var createResponses = new java.util.ArrayList<CreateUserResponse>();
            for (var f : futures) {
                var response = f.get(10, java.util.concurrent.TimeUnit.SECONDS);
                createResponses.add(response);
                userIds.add(response.getUser().getId());
            }

            SoftAssertions.assertSoftly(softly -> {
                for (var response : createResponses) {
                    softly.assertThat(response.getSuccess()).isTrue();
                }
                // Verify all users exist
                for (var userId : userIds) {
                    var getRequest = GetUserRequest.newBuilder()
                            .setId(userId)
                            .build();
                    var getResponse = userServiceClient.getUser(getRequest);
                    softly.assertThat(getResponse.getFound()).isTrue();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Concurrent gRPC calls failed", e);
        } finally {
            // Cleanup - delete all created users
            for (var userId : userIds) {
                try {
                    var deleteRequest = DeleteUserRequest.newBuilder()
                            .setId(userId)
                            .build();
                    userServiceClient.deleteUser(deleteRequest);
                } catch (Exception ignore) {
                    // best-effort cleanup
                }
            }
            pool.shutdownNow();
        }
    }

    @Test(timeOut = 15000)
    @Description("Awaitility example: wait until users array is retrievable via stream")
    @Severity(SeverityLevel.MINOR)
    public void awaitilityArrayStreamExampleGrpcTest() throws InterruptedException {
        // Create multiple users via stream
        List<CreateUserRequest> requests = IntStream.range(0, 3)
                .mapToObj(i -> CreateUserRequest.newBuilder()
                        .setName(TestDataGenerator.randomName())
                        .setEmail(TestDataGenerator.randomEmail())
                        .setAge(TestDataGenerator.randomAge())
                        .build())
                .collect(Collectors.toList());

        List<String> userIds = new ArrayList<>();
        try {
            CreateUsersStreamResponse createResponse = userServiceClient.createUsersStream(requests);
            userIds = createResponse.getResponsesList().stream()
                    .filter(CreateUserResponse::getSuccess)
                    .map(r -> r.getUser().getId())
                    .collect(Collectors.toList());

            // Await until all users are retrievable via stream
            final List<String> idsForAwait = new ArrayList<>(userIds); // make effectively final for lambda
            org.awaitility.Awaitility.await("users array should be retrievable via stream")
                    .atMost(java.time.Duration.ofSeconds(10))
                    .pollInterval(java.time.Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        List<GetUserRequest> getRequests = idsForAwait.stream()
                                .map(id -> GetUserRequest.newBuilder().setId(id).build())
                                .collect(Collectors.toList());
                        
                        List<GetUserResponse> getResponses = userServiceClient.getUsersStream(getRequests);
                        
                        SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(getResponses).hasSize(idsForAwait.size());
                            for (int i = 0; i < getResponses.size(); i++) {
                                GetUserResponse response = getResponses.get(i);
                                String expectedId = idsForAwait.get(i);
                                softly.assertThat(response.getFound()).isTrue();
                                softly.assertThat(response.getUser().getId()).isEqualTo(expectedId);
                            }
                        });
                    });
        } finally {
            if (!userIds.isEmpty()) {
                try {
                    List<DeleteUserRequest> deleteRequests = userIds.stream()
                            .map(id -> DeleteUserRequest.newBuilder().setId(id).build())
                            .collect(Collectors.toList());
                    userServiceClient.deleteUsersStream(deleteRequests);
                } catch (Exception ignore) {}
            }
        }
    }
}
