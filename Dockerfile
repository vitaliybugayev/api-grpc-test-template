# Multi-stage Docker build for Java test automation project
# Stage 1: Build environment with all dependencies
FROM eclipse-temurin:21-jdk-jammy AS build

# Install required system dependencies
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml ./
COPY test-suites/ ./test-suites/

# Download dependencies (for better layer caching)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Generate protobuf files and compile
RUN mvn clean compile test-compile -DskipTests

# Stage 2: Runtime environment (smaller image)
FROM eclipse-temurin:21-jre-jammy AS runtime

# Install system dependencies for test execution
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    libnss3 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxkbcommon0 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# Install Maven (required for test execution)
ARG MAVEN_VERSION=3.9.6
RUN wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn \
    && rm /tmp/maven.tar.gz

# Set Maven environment
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

# Create non-root user for security
RUN groupadd -r testuser && useradd -r -g testuser testuser

# Set working directory
WORKDIR /app

# Copy built application from build stage
COPY --from=build /app ./
COPY --from=build /root/.m2 /home/testuser/.m2

# Copy test execution scripts
COPY run-tests.sh ./
RUN chmod +x run-tests.sh

# Change ownership to testuser
RUN chown -R testuser:testuser /app /home/testuser/.m2

# Switch to non-root user
USER testuser

# Create directories for test results and reports
RUN mkdir -p /app/target/allure-results \
    && mkdir -p /app/target/surefire-reports \
    && mkdir -p /app/logs

# Environment variables for test configuration
ENV JAVA_OPTS="-Xmx2g -Xms1g"
ENV MAVEN_OPTS="-Xmx2g -Xms1g"

# Default test suite (can be overridden)
ENV TEST_SUITE=smoke.xml

# Expose ports for potential web interfaces (Allure serve, etc.)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Default command - run tests
CMD ["./run-tests.sh"]