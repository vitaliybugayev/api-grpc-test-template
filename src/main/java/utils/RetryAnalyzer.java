package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(RetryAnalyzer.class);
    
    private int retryCount = 0;
    private final int maxRetryCount;

    public RetryAnalyzer() {
        this.maxRetryCount = PropertiesManager.getIntProperty("test.retry.maxCount", 2);
    }

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            retryCount++;
            logger.info("Retrying test '{}' (attempt {}/{})", 
                       result.getMethod().getMethodName(), 
                       retryCount, 
                       maxRetryCount);
            return true;
        }
        
        logger.warn("Test '{}' failed after {} attempts", 
                   result.getMethod().getMethodName(), 
                   retryCount);
        return false;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }
}