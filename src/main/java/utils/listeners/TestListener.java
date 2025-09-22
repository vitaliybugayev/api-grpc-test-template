package utils.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TestListener implements ITestListener {
    private static final Logger LOG = LoggerFactory.getLogger(TestListener.class);
    
    private static final ConcurrentHashMap<String, AtomicInteger> retryMetrics = new ConcurrentHashMap<>();
    private static final AtomicInteger totalRetries = new AtomicInteger(0);
    private static final AtomicInteger totalRetriedTests = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, AtomicInteger> attemptCounters = new ConcurrentHashMap<>();

    @Override
    public void onTestStart(ITestResult result) {
        String testKey = getTestKey(result);
        int attempt = attemptCounters.computeIfAbsent(testKey, k -> new AtomicInteger(0)).incrementAndGet();
        LOG.info("Starting test: {} (attempt: {})", testKey, attempt);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testKey = getTestKey(result);
        int attempt = getCurrentAttempt(result);
        
        if (attempt > 1) {
            LOG.info("Test PASSED after {} attempts: {}", attempt, testKey);
            recordRetrySuccess(testKey, attempt - 1);
        } else {
            LOG.info("Test PASSED: {}", testKey);
        }
        attemptCounters.remove(testKey);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testKey = getTestKey(result);
        int attempt = getCurrentAttempt(result);

        int maxRetries = resolveMaxRetries(result); // number of retries after first attempt
        int maxAttempts = maxRetries + 1;           // total attempts including first
        boolean willRetry = attempt <= maxRetries;  // e.g., attempt 1 with maxRetries 2 -> will retry

        if (willRetry) {
            LOG.warn("Test FAILED (will retry): {} - attempt {}/{} - {}",
                    testKey,
                    attempt,
                    maxAttempts,
                    result.getThrowable() != null ? result.getThrowable().getMessage() : "no message");
            recordRetryAttempt(testKey);
        } else {
            if (attempt > 1) {
                LOG.error("Test FAILED after {} attempts: {} - {}",
                        attempt, testKey, result.getThrowable() != null ? result.getThrowable().getMessage() : "no message");
                recordRetryFailure(testKey, attempt - 1);
            } else {
                LOG.error("Test FAILED: {} - {}",
                        testKey, result.getThrowable() != null ? result.getThrowable().getMessage() : "no message");
            }
            attemptCounters.remove(testKey);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("Test SKIPPED: {}", getTestKey(result));
    }

    private String getTestKey(ITestResult result) {
        Object[] params = result.getParameters();
        String paramHash = (params != null && params.length > 0) ? ("#" + Arrays.deepHashCode(params)) : "";
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName() + paramHash;
    }

    private int getCurrentAttempt(ITestResult result) {
        AtomicInteger ai = attemptCounters.get(getTestKey(result));
        return ai != null ? ai.get() : 1;
    }

    private int resolveMaxRetries(ITestResult result) {
        try {
            // Try TestNG <= 7.x style: getRetryAnalyzer()
            Method m = result.getMethod().getClass().getMethod("getRetryAnalyzer");
            Object analyzer = m.invoke(result.getMethod());
            if (analyzer instanceof IRetryAnalyzer) {
                // Try to read getMaxRetryCount if available (our RetryAnalyzer exposes it)
                try {
                    Method maxM = analyzer.getClass().getMethod("getMaxRetryCount");
                    Object v = maxM.invoke(analyzer);
                    if (v instanceof Integer) return (Integer) v;
                } catch (NoSuchMethodException ignored) { }
                return 0;
            }
        } catch (NoSuchMethodException e) {
            // Try newer API: getRetryAnalyzerClass()
            try {
                Method m2 = result.getMethod().getClass().getMethod("getRetryAnalyzerClass");
                Object clsObj = m2.invoke(result.getMethod());
                if (clsObj instanceof Class<?>) {
                    Class<?> cls = (Class<?>) clsObj;
                    if (IRetryAnalyzer.class.isAssignableFrom(cls)) {
                        try {
                            Object analyzer = cls.getDeclaredConstructor().newInstance();
                            try {
                                Method maxM = analyzer.getClass().getMethod("getMaxRetryCount");
                                Object v = maxM.invoke(analyzer);
                                if (v instanceof Integer) return (Integer) v;
                            } catch (NoSuchMethodException ignored) { }
                        } catch (Exception ignored) { }
                    }
                }
            } catch (Exception ignored) { }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void recordRetryAttempt(String testKey) {
        retryMetrics.computeIfAbsent(testKey, k -> new AtomicInteger(0)).incrementAndGet();
        totalRetries.incrementAndGet();
    }

    private void recordRetrySuccess(String testKey, int retryCount) {
        totalRetriedTests.incrementAndGet();
        LOG.info("Retry metrics for {}: {} retries before success", testKey, retryCount);
    }

    private void recordRetryFailure(String testKey, int retryCount) {
        totalRetriedTests.incrementAndGet();
        LOG.warn("Retry metrics for {}: {} retries before final failure", testKey, retryCount);
    }

    public static void printRetryMetrics() {
        LOG.info("=== RETRY METRICS SUMMARY ===");
        LOG.info("Total tests that required retries: {}", totalRetriedTests.get());
        LOG.info("Total retry attempts made: {}", totalRetries.get());
        
        if (!retryMetrics.isEmpty()) {
            LOG.info("Per-test retry details:");
            retryMetrics.forEach((testKey, attempts) -> 
                LOG.info("  {}: {} retry attempts", testKey, attempts.get())
            );
        }
        LOG.info("=== END RETRY METRICS ===");
    }
}
