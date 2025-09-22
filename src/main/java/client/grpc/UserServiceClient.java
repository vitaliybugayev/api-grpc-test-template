package client.grpc;
import com.example.grpc.userservice.UserServiceGrpc;
import com.example.grpc.userservice.UserServiceProto.*;
import io.grpc.stub.StreamObserver;
import io.qameta.allure.Step;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.ArrayList;

public class UserServiceClient {
    private static final Logger LOG = Logger.getLogger(UserServiceClient.class.getName());
    private final BaseGrpcClient baseClient;
    private final UserServiceGrpc.UserServiceBlockingStub stub;
    private final UserServiceGrpc.UserServiceStub asyncStub;

    public UserServiceClient() {
        this.baseClient = new BaseGrpcClient("user.service");
        var deadlineSec = resolveDeadlineSeconds("user.service");
        var s = UserServiceGrpc.newBlockingStub(baseClient.getChannel());
        if (deadlineSec != null && deadlineSec > 0) {
            s = s.withDeadlineAfter(deadlineSec, TimeUnit.SECONDS);
            LOG.info("Blocking stub deadline enabled: " + deadlineSec + "s");
        }
        this.stub = s;
        this.asyncStub = UserServiceGrpc.newStub(baseClient.getChannel());
        LOG.info("UserServiceClient initialized");
    }

    public UserServiceClient(String host, int port) {
        this.baseClient = new BaseGrpcClient(host, port);
        var deadlineSec = resolveDeadlineSeconds(null);
        var s = UserServiceGrpc.newBlockingStub(baseClient.getChannel());
        if (deadlineSec != null && deadlineSec > 0) {
            s = s.withDeadlineAfter(deadlineSec, TimeUnit.SECONDS);
            LOG.info("Blocking stub deadline enabled: " + deadlineSec + "s");
        }
        this.stub = s;
        this.asyncStub = UserServiceGrpc.newStub(baseClient.getChannel());
        LOG.info("UserServiceClient initialized with host: " + host + " port: " + port);
    }

    private static Long resolveDeadlineSeconds(String servicePropertyPrefix) {
        try {
            if (servicePropertyPrefix != null && !servicePropertyPrefix.isEmpty()) {
                var envPrefix = servicePropertyPrefix.toUpperCase().replaceAll("[^A-Z0-9]", "_");
                var envKey = envPrefix + "_DEADLINE_SECONDS"; // e.g. USER_SERVICE_DEADLINE_SECONDS
                var envVal = System.getenv(envKey);
                if (envVal != null && !envVal.isBlank()) {
                    return Long.parseLong(envVal.trim());
                }
                var propKey = servicePropertyPrefix + ".deadline.seconds"; // e.g. user.service.deadline.seconds
                var propVal = utils.PropertiesManager.getProperty(propKey);
                if (propVal != null && !propVal.isBlank()) {
                    return Long.parseLong(propVal.trim());
                }
            }
            // Global fallback
            var globalEnv = System.getenv("GRPC_DEADLINE_SECONDS");
            if (globalEnv != null && !globalEnv.isBlank()) {
                return Long.parseLong(globalEnv.trim());
            }
            var globalProp = utils.PropertiesManager.getProperty("grpc.deadline.seconds");
            if (globalProp != null && !globalProp.isBlank()) {
                return Long.parseLong(globalProp.trim());
            }
        } catch (NumberFormatException ignore) {
            LOG.warning("Invalid gRPC deadline seconds value. Ignoring.");
        } catch (Exception ignore) {
            // property may be missing; ignore
        }
        return null;
    }

    @Step("Get user by ID: {request.id}")
    public GetUserResponse getUser(GetUserRequest request) {
        LOG.info("Getting user with ID: " + request.getId());
        try {
        var response = stub.getUser(request);
            LOG.info("User retrieval completed. Found: " + response.getFound());
            return response;
        } catch (Exception e) {
            LOG.severe("Failed to get user: " + e.getMessage());
            throw e;
        }
    }

    @Step("Create user: {request.name}")
    public CreateUserResponse createUser(CreateUserRequest request) {
        LOG.info("Creating user with name: " + request.getName());
        try {
            var response = stub.createUser(request);
            LOG.info("User creation completed. Success: " + response.getSuccess());
            return response;
        } catch (Exception e) {
            LOG.severe("Failed to create user: " + e.getMessage());
            throw e;
        }
    }

    @Step("Update user: {request.id}")
    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        LOG.info("Updating user with ID: " + request.getId());
        try {
            var response = stub.updateUser(request);
            LOG.info("User update completed. Success: " + response.getSuccess());
            return response;
        } catch (Exception e) {
            LOG.severe("Failed to update user: " + e.getMessage());
            throw e;
        }
    }

    @Step("Delete user: {request.id}")
    public DeleteUserResponse deleteUser(DeleteUserRequest request) {
        LOG.info("Deleting user with ID: " + request.getId());
        try {
            var response = stub.deleteUser(request);
            LOG.info("User deletion completed. Success: " + response.getSuccess());
            return response;
        } catch (Exception e) {
            LOG.severe("Failed to delete user: " + e.getMessage());
            throw e;
        }
    }

    @Step("List users with page: {request.page}, size: {request.size}")
    public ListUsersResponse listUsers(ListUsersRequest request) {
        LOG.info("Listing users with page: " + request.getPage() + ", size: " + request.getSize());
        try {
            var response = stub.listUsers(request);
            LOG.info("User listing completed. Total count: " + response.getTotalCount());
            return response;
        } catch (Exception e) {
            LOG.severe("Failed to list users: " + e.getMessage());
            throw e;
        }
    }

    @Step("Create users stream")
    public CreateUsersStreamResponse createUsersStream(List<CreateUserRequest> requests) throws InterruptedException {
        LOG.info("Creating users via stream with " + requests.size() + " requests");
        
        List<CreateUsersStreamResponse> responses = executeStream(
            requests,
            asyncStub::createUsersStream,
            "Users stream creation completed. Total processed: "
        );
        
        if (responses.isEmpty()) {
            throw new RuntimeException("No response received");
        }
        
        LOG.info("Users stream creation completed. Total processed: " + responses.get(0).getTotalProcessed());
        return responses.get(0);
    }
    
    @Step("Get users stream")
    public List<GetUserResponse> getUsersStream(List<GetUserRequest> requests) throws InterruptedException {
        LOG.info("Getting users via stream with " + requests.size() + " requests");
        
        List<GetUserResponse> responses = executeStream(
            requests,
            asyncStub::getUsersStream,
            "Users stream get completed. Received "
        );
        
        LOG.info("Users stream get completed. Received " + responses.size() + " responses");
        return responses;
    }
    
    @Step("Update users stream")
    public List<UpdateUserResponse> updateUsersStream(List<UpdateUserRequest> requests) throws InterruptedException {
        LOG.info("Updating users via stream with " + requests.size() + " requests");
        
        List<UpdateUserResponse> responses = executeStream(
            requests,
            asyncStub::updateUsersStream,
            "Users stream update completed. Received "
        );
        
        LOG.info("Users stream update completed. Received " + responses.size() + " responses");
        return responses;
    }
    
    @Step("Delete users stream")
    public DeleteUsersStreamResponse deleteUsersStream(List<DeleteUserRequest> requests) throws InterruptedException {
        LOG.info("Deleting users via stream with " + requests.size() + " requests");
        
        List<DeleteUsersStreamResponse> responses = executeStream(
            requests,
            asyncStub::deleteUsersStream,
            "Users stream deletion completed. Total processed: "
        );
        
        if (responses.isEmpty()) {
            throw new RuntimeException("No response received");
        }
        
        LOG.info("Users stream deletion completed. Total processed: " + responses.get(0).getTotalProcessed());
        return responses.get(0);
    }

    public BaseGrpcClient getBaseClient() {
        return baseClient;
    }

    private static final int STREAM_TIMEOUT_SECONDS = 30;
    
    private <T, R> List<R> executeStream(
            List<T> requests,
            java.util.function.Function<StreamObserver<R>, StreamObserver<T>> streamFunction,
            String logPrefix) throws InterruptedException {
        
        CountDownLatch latch = new CountDownLatch(1);
        List<R> responses = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        
        StreamObserver<R> responseObserver = new StreamObserver<R>() {
            @Override
            public void onNext(R response) {
                responses.add(response);
            }
            
            @Override
            public void onError(Throwable t) {
                errors.add(t);
                latch.countDown();
            }
            
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
        
        StreamObserver<T> requestObserver = streamFunction.apply(responseObserver);
        
        try {
            for (T request : requests) {
                requestObserver.onNext(request);
            }
            requestObserver.onCompleted();
            
            if (!latch.await(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("Stream timeout");
            }
            
            if (!errors.isEmpty()) {
                throw new RuntimeException("Stream error: " + errors.get(0).getMessage(), errors.get(0));
            }
            
            return responses;
        } catch (Exception e) {
            requestObserver.onError(e);
            throw e;
        }
    }
    
    public void shutdown() {
        baseClient.shutdown();
    }
}
