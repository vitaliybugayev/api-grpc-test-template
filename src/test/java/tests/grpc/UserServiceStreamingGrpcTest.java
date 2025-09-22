package tests.grpc;

import base.BaseGrpcTest;
import client.grpc.UserServiceClient;
import com.example.grpc.userservice.UserServiceGrpc;
import com.example.grpc.userservice.UserServiceProto.*;
import io.qameta.allure.*;
import io.grpc.stub.StreamObserver;
import org.assertj.core.api.SoftAssertions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Epic("gRPC Testing")
@Feature("User gRPC Streaming")
@Story("Client-streaming partial failures and totals via gRPC")
public class UserServiceStreamingGrpcTest extends BaseGrpcTest {

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

    private static boolean streamingEnabled() {
        return Boolean.parseBoolean(System.getProperty("enableStreamingTests",
                System.getenv().getOrDefault("ENABLE_STREAMING_TESTS", "false")));
    }

    @Test
    @Description("Client streaming example: partial failures and totals")
    @Severity(SeverityLevel.MINOR)
    public void createUsersStreamPartialFailuresGrpcTest() throws InterruptedException {
        if (!streamingEnabled()) throw new SkipException("Streaming tests disabled");

        var latch = new CountDownLatch(1);
        final CreateUsersStreamResponse[] holder = new CreateUsersStreamResponse[1];

        var async = UserServiceGrpc.newStub(client.getBaseClient().getChannel());

        StreamObserver<CreateUsersStreamResponse> responseObserver = new StreamObserver<>() {
            @Override public void onNext(CreateUsersStreamResponse value) { holder[0] = value; }
            @Override public void onError(Throwable t) { latch.countDown(); }
            @Override public void onCompleted() { latch.countDown(); }
        };

        var requestObserver = async.createUsersStream(responseObserver);
        // two valid, one invalid
        requestObserver.onNext(CreateUserRequest.newBuilder().setName("S1").setEmail("s1@example.com").setAge(20).build());
        requestObserver.onNext(CreateUserRequest.newBuilder().setName("").setEmail("bad").setAge(-1).build());
        requestObserver.onNext(CreateUserRequest.newBuilder().setName("S3").setEmail("s3@example.com").setAge(22).build());
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);

        var resp = holder[0];
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resp).as("response present").isNotNull();
            if (resp != null) {
                softly.assertThat(resp.getTotalProcessed()).isEqualTo(3);
                softly.assertThat(resp.getResponsesCount()).isGreaterThan(0);
            }
        });
    }
}

