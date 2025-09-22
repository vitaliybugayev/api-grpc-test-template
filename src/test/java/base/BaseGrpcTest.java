package base;

import client.grpc.BaseGrpcClient;
import io.qameta.allure.Step;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BaseGrpcTest extends BaseTest {
    protected static final Logger GRPC_LOG = LoggerFactory.getLogger(BaseGrpcTest.class);
    
    private List<BaseGrpcClient> grpcClients = new ArrayList<>();

    protected void registerGrpcClient(BaseGrpcClient client) {
        grpcClients.add(client);
    }

    @AfterClass(alwaysRun = true)
    public void shutdownGrpcClients() {
        for (var client : grpcClients) {
            try {
                client.shutdown();
            } catch (Exception e) {
                GRPC_LOG.warn("Failed to shutdown gRPC client: {}", e.getMessage());
            }
        }
        grpcClients.clear();
    }

    @Step("Wait for condition with timeout")
    protected void waitForCondition(Runnable condition, String description, int timeoutSeconds) {
        Awaitility.await(description)
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> condition.run());
    }

    @Step("Sleep for {0} milliseconds")
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleep was interrupted", e);
        }
    }
}
