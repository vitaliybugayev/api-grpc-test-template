package base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import utils.PropertiesManager;
import utils.listeners.TestListener;

@Listeners(TestListener.class)
public class BaseTest {
    protected static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    @BeforeSuite(alwaysRun = true)
    public static void init() {
        LOG.info("BaseTest: Initiating RestAssured");
        var baseUri = resolveApiHost();
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalStateException("api.host is not configured (system property, env var API_HOST, or /envs/<env>.properties)");
        }
        RestAssured.baseURI = baseUri;
        LOG.info("BaseTest: baseURI set to {}", RestAssured.baseURI);
        RestAssured.filters(new AllureRestAssured());
        
        // Enable logging for failed requests
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                // Uncomment to enable full logging
                // .addFilter(new RequestLoggingFilter())
                // .addFilter(new ResponseLoggingFilter())
                .build();
    }

    private static String resolveApiHost() {
        var sys = System.getProperty("api.host");
        if (sys != null && !sys.isBlank()) return sys;
        var env = System.getenv("API_HOST");
        if (env != null && !env.isBlank()) return env;
        return PropertiesManager.getProperty("api.host");
    }
}
