API/gRPC Test Template

Lean template for REST and gRPC testing with TestNG, AssertJ, Rest-Assured, and Allure.

Stack
- Java 21, Maven
- TestNG, AssertJ (SoftAssertions), Awaitility
- REST Assured, Jackson/Gson
- gRPC (protobuf, stubs via Maven plugin)
- Allure TestNG

Layout
```
src/
├── main/java/
│   ├── client/grpc/           # gRPC clients
│   ├── models/                # Records for API payloads
│   ├── steps/                 # REST Assured steps
│   └── utils/                 # RetryAnalyzer, listeners, etc.
├── main/proto/                # .proto files
├── main/resources/
│   ├── envs/                  # env configs (dev, staging)
│   └── test.properties        # base test configuration
├── test/java/
│   ├── tests/                 # api, grpc, integration tests
│   └── examples/              # retry examples
└── TESTING_GUIDELINES.md      # annotation usage guidelines
```

Configuration
- Environment: pass `-Denv=dev|staging` to load `envs/<env>.properties`.
- API base URI: from `api.host` in properties, can be overridden by system property `-Dapi.host=<url>` or env var `API_HOST`. API version via `-Dversion=v1`.
- gRPC client host/port:
  - Properties: `user.service.host`, `user.service.port` (from env file).
  - Environment variables override properties: `USER_SERVICE_HOST`, `USER_SERVICE_PORT`.
  - Mapping rule: `a.b` → `A_B_HOST`/`A_B_PORT`.
  - Deadlines (blocking stub): set via `user.service.deadline.seconds` or globally `grpc.deadline.seconds`; can be overridden by env vars `USER_SERVICE_DEADLINE_SECONDS` or `GRPC_DEADLINE_SECONDS`.
- Properties loading order: `test.properties` (optional) → `envs/<env>.properties` (overrides). System properties and environment variables override both where applicable.
- API version is taken from `-Dversion` (default: `v1`). The `api.version` key in env files is currently informational and not wired.

Protobuf (required)
- Build stubs with Maven (compiles `.proto` and generates Java sources):
  - `mvn clean compile`
- Generated sources are placed under:
  - `target/generated-sources/protobuf/java`
  - `target/generated-sources/protobuf/grpc-java`
- The protobuf-maven-plugin config adds them to the build path. If your IDE does not pick them up, mark both folders as Generated Sources (e.g., in IntelliJ IDEA: Mark Directory as Generated Sources Root).

Run
- Compile only: `mvn -DskipTests compile`
- Smoke tests: `mvn test -DsuiteXmlFile=smoke.xml`
- Full tests: `mvn test -DsuiteXmlFile=full.xml`
- Integration: `mvn test -DsuiteXmlFile=integration.xml`
- With env and gRPC overrides:
  - `export USER_SERVICE_HOST=localhost; export USER_SERVICE_PORT=9001`
  - `mvn test -Denv=dev -DsuiteXmlFile=smoke.xml`
- Enable streaming tests (optional):
  - System property: `mvn test -DsuiteXmlFile=full.xml -DenableStreamingTests=true`
  - Or environment variable: `export ENABLE_STREAMING_TESTS=true`

Conventions that matter
- **Test naming**: Use format `{action}{Entity}{Type}Test()` - e.g., `createUserApiTest()`, `concurrentGrpcCallsTest()`
- **Soft assertions**: Use `SoftAssertions.assertSoftly(softly -> { ... });` (see tests)
- **Retry mechanism**: Use `@Test(retryAnalyzer = RetryAnalyzer.class)` for flaky tests; max attempts are configured via `test.retry.maxCount` in `test.properties` (default: 2). Avoid masking real defects with retries; prefer fixing root causes.
- **Annotations**: Follow `TESTING_GUIDELINES.md` for `@Severity`, `priority`, and `@Flaky` usage
- **Awaitility**: `UserServiceGrpcTest.awaitilityArrayStreamExampleGrpcTest` shows `await().untilAsserted(...)` for eventual consistency
- **gRPC client**: `UserServiceClient` uses env/properties and is closed in tests via `BaseGrpcTest`
- **Allure**: TestNG adapter + @Step annotations for both REST and gRPC
- **REST logging**: BaseTest enables `RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()` and registers `AllureRestAssured` filter
