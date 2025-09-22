package tests.grpc;

import base.BaseGrpcTest;
import client.grpc.UserServiceClient;
import com.example.grpc.userservice.UserServiceGrpc;
import com.example.grpc.userservice.UserServiceProto.*;
import io.grpc.StatusRuntimeException;
import io.qameta.allure.*;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

@Epic("gRPC Testing")
@Feature("User gRPC Negative")
@Story("Validation and not-found flows via gRPC")
public class UserServiceNegativeGrpcTest extends BaseGrpcTest {

    private UserServiceClient client;

    @BeforeClass
    public void setUp() {
        client = new UserServiceClient();
        registerGrpcClient(client.getBaseClient());
    }

    @AfterClass
    public void tearDown() {
        if (client != null) client.shutdown();
    }

    @Test
    @Description("Get user with malformed ID: expect NOT_FOUND/INVALID_ARGUMENT or found=false")
    @Severity(SeverityLevel.NORMAL)
    public void getUserMalformedIdGrpcTest() {
        var req = GetUserRequest.newBuilder().setId("not-a-uuid").build();
        try {
            var resp = client.getUser(req);
            SoftAssertions.assertSoftly(softly -> softly.assertThat(resp.getFound()).isFalse());
        } catch (StatusRuntimeException e) {
            SoftAssertions.assertSoftly(softly -> softly.assertThat(e.getStatus().getCode())
                    .isIn(io.grpc.Status.Code.INVALID_ARGUMENT, io.grpc.Status.Code.NOT_FOUND));
        }
    }

    @Test
    @Description("Delete non-existent user: expect NOT_FOUND or success=false")
    @Severity(SeverityLevel.NORMAL)
    public void deleteUserNotFoundGrpcTest() {
        var req = DeleteUserRequest.newBuilder().setId("00000000-0000-0000-0000-000000000000").build();
        try {
            var resp = client.deleteUser(req);
            SoftAssertions.assertSoftly(softly -> softly.assertThat(resp.getSuccess()).isFalse());
        } catch (StatusRuntimeException e) {
            SoftAssertions.assertSoftly(softly -> softly.assertThat(e.getStatus().getCode())
                    .isIn(io.grpc.Status.Code.NOT_FOUND));
        }
    }

    @Test
    @Description("Update user with invalid email: expect INVALID_ARGUMENT or success=false")
    @Severity(SeverityLevel.NORMAL)
    public void updateUserInvalidEmailGrpcTest() {
        // Precondition: create a valid user
        var create = CreateUserRequest.newBuilder()
                .setName("Neg Update")
                .setEmail("neg.update@example.com")
                .setAge(22)
                .build();
        var created = client.createUser(create);
        var userId = created.getUser().getId();
        try {
            var update = UpdateUserRequest.newBuilder()
                    .setId(userId)
                    .setName("Neg Update")
                    .setEmail("bad-email")
                    .setAge(22)
                    .setStatus(UserStatus.ACTIVE)
                    .build();
            try {
                var resp = client.updateUser(update);
                SoftAssertions.assertSoftly(softly -> softly.assertThat(resp.getSuccess()).isFalse());
            } catch (StatusRuntimeException e) {
                SoftAssertions.assertSoftly(softly -> softly.assertThat(e.getStatus().getCode())
                        .isIn(io.grpc.Status.Code.INVALID_ARGUMENT));
            }
        } finally {
            client.deleteUser(DeleteUserRequest.newBuilder().setId(userId).build());
        }
    }

    @Test
    @Description("Deadline example: tiny deadline may cause DEADLINE_EXCEEDED")
    @Severity(SeverityLevel.MINOR)
    public void listUsersDeadlineExceededGrpcTest() {
        var channel = client.getBaseClient().getChannel();
        var stub = UserServiceGrpc.newBlockingStub(channel).withDeadlineAfter(1, TimeUnit.MILLISECONDS);
        var req = ListUsersRequest.newBuilder().setPage(1).setSize(50).setFilter("").build();
        try {
            var resp = stub.listUsers(req);
            SoftAssertions.assertSoftly(softly -> softly.assertThat(resp).isNotNull());
        } catch (StatusRuntimeException e) {
            SoftAssertions.assertSoftly(softly -> softly.assertThat(e.getStatus().getCode())
                    .isEqualTo(io.grpc.Status.Code.DEADLINE_EXCEEDED));
        }
    }
}

