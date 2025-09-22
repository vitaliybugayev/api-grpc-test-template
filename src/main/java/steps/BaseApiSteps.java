package steps;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class BaseApiSteps {

    @Step("GET {path}")
    protected Response doGet(String path) {
        return given()
                .when()
                .get(path)
                .then()
                .extract()
                .response();
    }

    @Step("GET {path} with params")
    protected Response doGet(String path, Map<String, ?> pathParams, Map<String, ?> queryParams) {
        var spec = given();
        if (pathParams != null && !pathParams.isEmpty()) {
            spec = spec.pathParams(pathParams);
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            spec = spec.queryParams(queryParams);
        }
        return spec
                .when()
                .get(path)
                .then()
                .extract()
                .response();
    }

    @Step("POST {path}")
    protected Response doPost(String path, Object body) {
        return given()
                .body(body)
                .when()
                .post(path)
                .then()
                .extract()
                .response();
    }

    @Step("PUT {path}")
    protected Response doPut(String path, Map<String, ?> pathParams, Object body) {
        var spec = given();
        if (pathParams != null && !pathParams.isEmpty()) {
            spec = spec.pathParams(pathParams);
        }
        return spec
                .body(body)
                .when()
                .put(path)
                .then()
                .extract()
                .response();
    }

    @Step("DELETE {path}")
    protected Response doDelete(String path, Map<String, ?> pathParams) {
        var spec = given();
        if (pathParams != null && !pathParams.isEmpty()) {
            spec = spec.pathParams(pathParams);
        }
        return spec
                .when()
                .delete(path)
                .then()
                .extract()
                .response();
    }
}

