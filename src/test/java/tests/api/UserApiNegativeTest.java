package tests.api;

import base.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import models.requests.CreateUserRequest;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import steps.UserApiSteps;

@Epic("API Testing")
@Feature("User API Negative")
@Story("Validation and error handling via REST API")
public class UserApiNegativeTest extends BaseTest {

    private final UserApiSteps steps = new UserApiSteps();

    @Test
    @Description("Create user with invalid email should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createUserInvalidEmailApiTest() {
        var req = CreateUserRequest.builder()
                .name("Invalid Email")
                .email("not-an-email")
                .age(25)
                .build();

        Response resp = steps.createUserRaw(req);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resp.statusCode()).isEqualTo(400);
            softly.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
        });
    }

    @Test
    @Description("Create user with missing name should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createUserMissingNameApiTest() {
        var req = CreateUserRequest.builder()
                .name("")
                .email("valid@example.com")
                .age(25)
                .build();

        Response resp = steps.createUserRaw(req);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resp.statusCode()).isEqualTo(400);
            softly.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
        });
    }

    @Test
    @Description("Create user with age out of range should return 400")
    @Severity(SeverityLevel.NORMAL)
    public void createUserAgeOutOfRangeApiTest() {
        var req = CreateUserRequest.builder()
                .name("Age Too Low")
                .email("age@example.com")
                .age(-1)
                .build();

        Response resp = steps.createUserRaw(req);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resp.statusCode()).isEqualTo(400);
            softly.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
        });
    }

    @Test
    @Description("Duplicate email should return 409 Conflict")
    @Severity(SeverityLevel.NORMAL)
    public void createUserDuplicateEmailApiTest() {
        var uniqueEmail = "dup." + java.util.UUID.randomUUID() + "@example.com";
        var base = CreateUserRequest.builder()
                .name("Dup Email")
                .email(uniqueEmail)
                .age(30)
                .build();

        // First create should succeed
        var first = steps.createUser(base);
        var createdId = first.jsonPath().getString("data.id");
        try {
            // Second create with same email should be 409
            Response second = steps.createUserRaw(base);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(second.statusCode()).isEqualTo(409);
            });
        } finally {
            // cleanup
            steps.deleteUser(createdId);
        }
    }

    @DataProvider(name = "paginationBadParams")
    public Object[][] paginationBadParams() {
        return new Object[][]{
                {0, 10},
                {-1, 10},
                {1, 0},
                {1, -5},
                {1, 10_000}
        };
    }

    @Test(dataProvider = "paginationBadParams")
    @Description("Pagination boundaries should be validated or normalized")
    @Severity(SeverityLevel.MINOR)
    public void getUsersListPaginationEdgesApiTest(int page, int size) {
        Response resp = steps.getUsersListRaw(page, size, "");
        // Template behavior: either 400/422 or 200 with normalized values
        if (resp.statusCode() == 200) {
            SoftAssertions.assertSoftly(softly -> {
                var bodyPage = resp.jsonPath().getInt("page");
                var bodySize = resp.jsonPath().getInt("size");
                softly.assertThat(bodyPage).isGreaterThan(0);
                softly.assertThat(bodySize).isGreaterThan(0);
            });
        } else {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(resp.statusCode()).isIn(400, 422);
            });
        }
    }
}
