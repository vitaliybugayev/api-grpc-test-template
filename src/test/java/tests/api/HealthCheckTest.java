package tests.api;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.RestAssured;
import org.testng.annotations.Test;
import utils.Endpoints;

import static org.hamcrest.Matchers.equalTo;

@Epic("API Testing")
@Feature("Health Check")
@Story("Service availability")
public class HealthCheckTest extends BaseTest {

    @Test
    @Description("Verify that health check endpoint returns 200 OK")
    @Severity(SeverityLevel.BLOCKER)
    public void healthCheckApiTest() {
        RestAssured.given()
                .when()
                .get(Endpoints.HEALTH_CHECK)
                .then()
                .statusCode(200)
                .body("status", equalTo("OK"));
    }
}
