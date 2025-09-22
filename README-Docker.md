# Docker Configuration for API gRPC Tests

This document describes the Docker setup for the Java test automation project.

## 🐳 Docker Setup

### Files Created

- **`Dockerfile`** - Multi-stage build for Java test environment
- **`docker-compose.yml`** - Complete development and testing environment
- **`run-tests.sh`** - Test execution script with error handling
- **`.dockerignore`** - Optimized build context

## 🚀 Quick Start

### Build and Run Tests

```bash
# Build Docker image
docker build -t api-grpc-tests .

# Run tests with default settings (smoke.xml)
docker run --rm api-grpc-tests

# Run specific test suite
docker run --rm -e TEST_SUITE=regression.xml api-grpc-tests

# Run with custom environment
docker run --rm \
  -e TEST_SUITE=full.xml \
  -e ENV=staging \
  -e USER_SERVICE_HOST=staging.example.com \
  -e USER_SERVICE_PORT=50051 \
  api-grpc-tests
```

### Using Docker Compose (Recommended)

```bash
# Run tests with default configuration
docker-compose up api-grpc-tests

# Run specific test suite
TEST_SUITE=integration.xml docker-compose up api-grpc-tests

# Run with Allure reporting UI
docker-compose --profile reports up

# Development mode (run mock service separately)
docker-compose up -d mock-service
docker-compose run --rm api-grpc-tests
```

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `TEST_SUITE` | `smoke.xml` | Test suite file to execute |
| `ENV` | `dev` | Test environment configuration |
| `PARALLEL_TESTS` | `1` | Number of parallel test threads |
| `RETRY_COUNT` | `2` | Number of test retries on failure |
| `USER_SERVICE_HOST` | - | gRPC service hostname |
| `USER_SERVICE_PORT` | `50051` | gRPC service port |
| `GRPC_DEADLINE_SECONDS` | `30` | gRPC request timeout |

### Available Test Suites

- `smoke.xml` - Quick smoke tests
- `regression.xml` - Full regression suite
- `integration.xml` - Integration tests
- `full.xml` - All tests

## 📊 Test Reports

### Local Volume Mounts

Test results are automatically saved to local directories:

```bash
./target/allure-results/    # Allure test results
./target/surefire-reports/  # Maven Surefire reports
./logs/                     # Test execution logs
```

### Allure Reports

```bash
# Generate and serve Allure report
docker-compose --profile reports up allure-ui

# Access report at: http://localhost:5050
```

## 🛠 Development Workflow

### 1. Local Development

```bash
# Run tests locally in Docker
docker-compose run --rm api-grpc-tests

# Run specific test class
docker-compose run --rm api-grpc-tests mvn test -Dtest=UserApiTest

# Debug mode with volume mount for live changes
docker-compose run --rm \
  -v $(pwd)/src:/app/src:ro \
  api-grpc-tests
```

### 2. CI/CD Pipeline

```bash
# Build and test in one command
docker build -t api-grpc-tests . && docker run --rm api-grpc-tests

# With test results extraction
docker run --rm \
  -v $(pwd)/reports:/app/target \
  api-grpc-tests
```

## 🏗 Docker Image Details

### Multi-Stage Build

1. **Build Stage** (`eclipse-temurin:21-jdk-jammy`)
   - Full JDK environment
   - Maven dependency resolution
   - Source compilation
   - Protobuf generation

2. **Runtime Stage** (`eclipse-temurin:21-jre-jammy`)
   - Lightweight JRE environment
   - Maven runtime only
   - Test execution optimized
   - Non-root security user

### Image Size Optimization

- Multi-stage build reduces final image size
- Layer caching for dependencies
- Minimal runtime dependencies
- Efficient `.dockerignore`

## 🔍 Troubleshooting

### Common Issues

1. **"Test suite not found"**
   ```bash
   # Check available test suites
   docker run --rm api-grpc-tests ls -la test-suites/
   ```

2. **gRPC connection failures**
   ```bash
   # Test gRPC connectivity
   docker run --rm api-grpc-tests nc -zv your-grpc-host 50051
   ```

3. **Memory issues**
   ```bash
   # Increase memory allocation
   docker run --rm -e JAVA_OPTS="-Xmx4g" api-grpc-tests
   ```

4. **Permission issues**
   ```bash
   # Check container user
   docker run --rm api-grpc-tests id
   ```

### Debug Mode

```bash
# Run with bash shell for debugging
docker run --rm -it --entrypoint=/bin/bash api-grpc-tests

# Check test environment inside container
docker run --rm api-grpc-tests ./run-tests.sh --help
```

## 📈 Performance Tuning

### Parallel Execution

```bash
# Run tests in parallel (4 threads)
docker run --rm -e PARALLEL_TESTS=4 api-grpc-tests
```

### Memory Configuration

```bash
# Optimize JVM memory settings
docker run --rm \
  -e JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC" \
  -e MAVEN_OPTS="-Xmx2g" \
  api-grpc-tests
```

## 🔐 Security Notes

- Container runs as non-root user `testuser`
- No sensitive data in Docker image
- Use environment variables for configuration
- Regular security updates for base images

## 📝 Maintenance

### Update Dependencies

```bash
# Rebuild with latest dependencies
docker build --no-cache -t api-grpc-tests .
```

### Clean Up

```bash
# Remove old containers and images
docker system prune

# Remove specific images
docker rmi api-grpc-tests
```