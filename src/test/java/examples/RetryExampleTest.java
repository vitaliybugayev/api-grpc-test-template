package examples;

import base.BaseTest;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import utils.RetryAnalyzer;

import java.net.SocketTimeoutException;
import java.util.Random;

@Epic("Retry Examples")
@Feature("Retry Mechanism Demonstration")
@Story("Retry behaviors and flakiness")
public class RetryExampleTest extends BaseTest {

    private final Random random = new Random();

    @Test(retryAnalyzer = RetryAnalyzer.class)
    @Description("Basic retry example - fails 50% of the time")
    @Severity(SeverityLevel.MINOR)
    public void basicRetryExampleTest() {
        if (random.nextBoolean()) {
            Assert.fail("Random failure for retry demonstration");
        }
        // Test passes if random is false
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    @Description("Network retry example - simulates connection timeout")
    @Severity(SeverityLevel.NORMAL)
    public void networkRetryExampleTest() throws SocketTimeoutException {
        if (random.nextInt(4) != 0) { // 75% failure rate
            throw new java.net.SocketTimeoutException("Simulated network timeout");
        }
        // Test passes 25% of the time
    }

    @Test(retryAnalyzer = RetryAnalyzer.class, groups = {"quarantine"})
    @Description("Flaky test example - timing dependent")
    @Severity(SeverityLevel.MINOR)
    @Flaky
    public void flakyRetryExampleTest() {
        // Simulate timing-dependent test
        long currentTime = System.currentTimeMillis();
        if (currentTime % 3 != 0) { // Fails 2/3 of the time
            Assert.fail("Timing-dependent assertion failed at " + currentTime);
        }
        // Test passes when currentTime is divisible by 3
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    @Description("gRPC connection retry example")
    @Severity(SeverityLevel.NORMAL)
    public void grpcRetryExampleTest() {
        if (random.nextInt(3) != 0) { // 67% failure rate
            throw new io.grpc.StatusRuntimeException(
                io.grpc.Status.UNAVAILABLE.withDescription("Service temporarily unavailable")
            );
        }
        // Test passes 33% of the time
    }

    @Test
    @Description("Test that should NOT retry - logical error")
    @Severity(SeverityLevel.MINOR)
    public void noRetryExampleTest() {
        // This test has a logical error and should NOT use retry
        Assert.assertEquals(2 + 2, 5, "This is a logical error, not a flaky test");
    }
}
