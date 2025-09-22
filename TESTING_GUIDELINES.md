# Test Annotation Usage Guidelines

## @Severity - Criticality Levels

### BLOCKER
Use for environment/service availability checks that gate other suites.
- Health checks (service up), essential smoke verifications

**Example:**
```java
@Severity(SeverityLevel.BLOCKER)
public void healthCheckApiTest() {}
```

### CRITICAL
Critical tests for core functionality:
- User creation/deletion (CRUD lifecycle)
- Core streaming operations (bulk actions)
- Tests that verify key business logic

**Examples:**
```java
@Severity(SeverityLevel.CRITICAL)
public void createUserApiTest() {}

@Severity(SeverityLevel.CRITICAL)
public void deleteUserApiTest() {}

@Severity(SeverityLevel.CRITICAL)
public void createUsersStreamGrpcTest() {}
```

### NORMAL
Standard functional tests:
- Input data validation
- User updates
- Pagination and filtering
- Negative scenarios (404, conflicts)
- Boundary condition checks

**Examples:**
```java
@Severity(SeverityLevel.NORMAL)
public void updateUserApiTest() {}

@Severity(SeverityLevel.NORMAL)
public void getUserNotFoundApiTest() {}

@Severity(SeverityLevel.NORMAL)
public void createUserValidationErrorApiTest() {}
```

### MINOR
Helper tests and edge cases:
- Concurrent calls
- Performance tests
- Awaitility examples
- Pagination edge values

**Examples:**
```java
@Severity(SeverityLevel.MINOR)
public void concurrentGrpcCallsTest() {}

@Severity(SeverityLevel.MINOR)
public void getUsersListPaginationEdgesApiTest() {}
```

## priority - Execution Order

Use priorities sparingly; prefer independent tests and dependsOnMethods for essential sequences.

### 1-3: Critical tests (CRUD lifecycle)
- **1**: Resource creation
- **2**: Resource retrieval
- **3**: Resource updates

### 4-6: Standard functional tests
- Pagination, filtering
- Validation, negative scenarios

### 7+: Cleanup and additional tests
- Resource deletion
- Performance tests

**Example:**
```java
@Test(priority = 1)
public void createUserApiTest() {}

@Test(priority = 2, dependsOnMethods = "createUserApiTest")
public void getUserByIdApiTest() {}

@Test(priority = 7, dependsOnMethods = {"createUserApiTest", "getUserByIdApiTest"})
public void deleteUserApiTest() {}
```

## @Flaky - Unstable Tests

Quarantine group may be used in CI to exclude flakies from gating pipelines while tracking debt.

### When to use:
- Tests that depend on external services
- Tests with timing issues
- Tests that require retry mechanism

### When NOT to use:
- ❌ As an excuse for bad tests
- ❌ For tests with logical errors
- ❌ For tests that can be made stable

**Proper usage:**
```java
@Flaky // Only if truly necessary
@Test(retryAnalyzer = RetryAnalyzer.class)
public void externalServiceIntegrationTest() {
    // Test that depends on external API
}
```

## Retry Mechanism Implementation

### RetryAnalyzer

Simple retry mechanism for any test:
```java
@Test(retryAnalyzer = RetryAnalyzer.class)
public void anyTest() {
    // Configurable via test.retry.maxCount (default: 2)
}
```

### Configuration

Add to `test.properties`:
```properties
test.retry.maxCount=2
```

### Best Practices

1. **Use RetryAnalyzer** for any test that needs retry
2. **Use @Flaky annotation** to mark unstable tests
3. **Fix root causes** instead of relying on retries

## Proper Usage Examples

```java
@Test(priority = 1)
@Description("Create a new user via API")
@Severity(SeverityLevel.CRITICAL)
public void createUserApiTest() {
    // Critical creation test
}

@Test(priority = 2, dependsOnMethods = "createUserApiTest")
@Description("Get user by ID")
@Severity(SeverityLevel.CRITICAL)  
public void getUserByIdApiTest() {
    // Critical retrieval test
}

@Test(priority = 5)
@Description("Create user with invalid data")
@Severity(SeverityLevel.NORMAL)
@Issue("API-001")
public void createUserValidationErrorApiTest() {
    // Standard validation test
}

@Test
@Description("Test concurrent API calls")
@Severity(SeverityLevel.MINOR)
public void concurrentGrpcCallsTest() {
    // Additional concurrency test
}
```

---

## Assertion Strategy
- Use Rest-Assured for transport-level checks (status codes, headers, basic shape) inside “verified” step methods.
- Use AssertJ for business/data assertions in tests. Prefer `SoftAssertions.assertSoftly` for multiple related checks.
- For negative/variant scenarios, call the “raw” step methods and assert in the test body.

```java
var resp = userApiSteps.createUserRaw(req);
org.assertj.core.api.Assertions.assertThat(resp.statusCode()).isEqualTo(400);
org.assertj.core.api.SoftAssertions.assertSoftly(s -> {
  s.assertThat(resp.jsonPath().getBoolean("success")).isFalse();
  s.assertThat(resp.jsonPath().getString("error")).isNotBlank();
});
```

## Determinism, Parallelism, and Random Data
- Tests must be parallel-safe; no shared mutable state.
- Use random data helpers, but allow seeding via `-Drand.seed` to reproduce failures.
- Prefer unique prefixes (e.g., run ID) for created entities to avoid collisions and simplify cleanup. Use `-Dnamespaced=true` and optional `-Drun.id=<custom>`.

```java
long seed = Long.getLong("rand.seed", System.nanoTime());
java.util.random.RandomGenerator rnd = java.util.random.RandomGenerator.of("L64X256MixRandom");
```

## Timeouts and Waits
- Set explicit time bounds for potentially long operations: use TestNG’s `timeOut` for upper limits.
- Prefer Awaitility for eventual consistency with bounded `atMost` and small `pollInterval`.

```java
@Test(timeOut = 15000)
public void eventuallyConsistentFlow() {
  org.awaitility.Awaitility.await()
         .atMost(java.time.Duration.ofSeconds(10))
         .pollInterval(java.time.Duration.ofMillis(300))
         .untilAsserted(() -> {/* assertions */});
}
```

## Flaky Tests and Quarantine
- Mark true flakies with `@Flaky` and limit retries (`test.retry.maxCount`, default 2).
- Use TestNG groups to quarantine: `@Test(groups={"quarantine"})`. Exclude in CI by `-DexcludedGroups=quarantine` while still running nightly.

## Grouping, Ownership, and Traceability
- Required metadata: `@Epic`, `@Feature`, `@Story`, `@Severity`.
- Recommended: `@Owner("team|person")`, `@Issue("JIRA-123")`, `@TmsLink("TMS-456")`.
- Use groups for suites: `smoke`, `full`, `integration`, `quarantine`.

## Test Data Lifecycle and Cleanup
- Always clean up resources you create; use `try/finally`.
- Prefer idempotent cleanup (delete-if-exists) and register created IDs when doing bulk creates so failures don’t leave residue.
- Avoid coupling tests by sharing IDs across methods unless sequenced via `dependsOnMethods`.

## REST/gRPC Contract Checks
- REST: validate response shape with JSON schema for core endpoints; keep schemas versioned with the API.
- gRPC: handle `StatusRuntimeException` explicitly; document expected Status codes (e.g., `INVALID_ARGUMENT`, `NOT_FOUND`, `DEADLINE_EXCEEDED`). Prefer `withDeadlineAfter` from configuration.

```java
try { client.createUser(invalidReq); org.assertj.core.api.Assertions.fail("expected INVALID_ARGUMENT"); }
catch (io.grpc.StatusRuntimeException e) { org.assertj.core.api.Assertions.assertThat(e.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.INVALID_ARGUMENT); }
```

## Negative and Edge-Case Matrix
- REST: 400 validation errors; 404 not found; 409 conflict (duplicates); 401/403 when auth is present.
- gRPC: `INVALID_ARGUMENT` for validation; `NOT_FOUND` for missing; `FAILED_PRECONDITION` for state issues; `DEADLINE_EXCEEDED` for timeouts.
- Pagination: define valid ranges; assert normalization or proper error.

## Skips Policy
- Use `SkipException` only for toggled/unsupported scenarios (e.g., streaming disabled).
- Skipped tests must log the reason and be tracked to avoid silent rot.

## Performance-Lite Guidance
- Small throughput checks allowed (order-of-magnitude), but avoid brittle timing asserts.
- Prefer time bounds and size-based sanity checks over exact durations.