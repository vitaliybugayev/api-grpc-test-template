#!/bin/bash

# Test execution script for Docker container
# This script handles test execution with proper error handling and reporting

set -e  # Exit on any error

# Default values
TEST_SUITE="${TEST_SUITE:-smoke.xml}"
ENV="${ENV:-dev}"
PARALLEL_TESTS="${PARALLEL_TESTS:-1}"
RETRY_COUNT="${RETRY_COUNT:-2}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Print environment information
print_env_info() {
    log "=== Test Environment Information ==="
    log "Test Suite: $TEST_SUITE"
    log "Environment: $ENV"
    log "Java Version: $(java -version 2>&1 | head -n 1)"
    log "Maven Version: $(mvn -version 2>&1 | head -n 1)"
    log "Working Directory: $(pwd)"
    log "Available Memory: $(free -h | grep '^Mem:' || echo 'N/A')"
    log "====================================="
}

# Validate environment
validate_environment() {
    log "Validating environment..."
    
    # Check if required files exist
    if [[ ! -f "pom.xml" ]]; then
        error "pom.xml not found. Make sure you're in the correct directory."
        exit 1
    fi
    
    if [[ ! -f "test-suites/$TEST_SUITE" ]]; then
        error "Test suite file 'test-suites/$TEST_SUITE' not found."
        log "Available test suites:"
        ls -la test-suites/ || true
        exit 1
    fi
    
    # Create necessary directories
    mkdir -p target/allure-results
    mkdir -p target/surefire-reports
    mkdir -p logs
    
    success "Environment validation completed."
}

# Wait for dependencies (gRPC services, databases, etc.)
wait_for_dependencies() {
    log "Waiting for dependencies..."
    
    # Wait for gRPC service if configured
    if [[ -n "$USER_SERVICE_HOST" && -n "$USER_SERVICE_PORT" ]]; then
        log "Waiting for gRPC service at $USER_SERVICE_HOST:$USER_SERVICE_PORT..."
        
        timeout=60
        while ! nc -z "$USER_SERVICE_HOST" "$USER_SERVICE_PORT" 2>/dev/null; do
            if [[ $timeout -le 0 ]]; then
                warning "gRPC service not available at $USER_SERVICE_HOST:$USER_SERVICE_PORT"
                warning "Continuing with tests anyway..."
                break
            fi
            log "Waiting for gRPC service... ($timeout seconds remaining)"
            sleep 2
            ((timeout -= 2))
        done
        
        if nc -z "$USER_SERVICE_HOST" "$USER_SERVICE_PORT" 2>/dev/null; then
            success "gRPC service is available"
        fi
    fi
}

# Pre-test setup
pre_test_setup() {
    log "Performing pre-test setup..."
    
    # Clean previous results
    if [[ -d "target/allure-results" ]]; then
        rm -rf target/allure-results/*
    fi
    
    # Generate protobuf files if needed
    log "Generating protobuf files..."
    mvn protobuf:compile protobuf:compile-custom -q || {
        error "Failed to generate protobuf files"
        exit 1
    }
    
    success "Pre-test setup completed."
}

# Run tests with proper configuration
run_tests() {
    log "Starting test execution..."
    log "Test Suite: $TEST_SUITE"
    
    # Build Maven command
    mvn_cmd="mvn clean test"
    mvn_cmd="$mvn_cmd -DsuiteXmlFile=$TEST_SUITE"
    mvn_cmd="$mvn_cmd -Denv=$ENV"
    mvn_cmd="$mvn_cmd -Dtest.retry.maxCount=$RETRY_COUNT"
    
    # Add parallel execution if configured
    if [[ "$PARALLEL_TESTS" -gt 1 ]]; then
        mvn_cmd="$mvn_cmd -Dparallel=methods -DthreadCount=$PARALLEL_TESTS"
    fi
    
    # Add system properties for gRPC if configured
    if [[ -n "$USER_SERVICE_HOST" ]]; then
        mvn_cmd="$mvn_cmd -DUSER_SERVICE_HOST=$USER_SERVICE_HOST"
    fi
    if [[ -n "$USER_SERVICE_PORT" ]]; then
        mvn_cmd="$mvn_cmd -DUSER_SERVICE_PORT=$USER_SERVICE_PORT"
    fi
    if [[ -n "$GRPC_DEADLINE_SECONDS" ]]; then
        mvn_cmd="$mvn_cmd -DGRPC_DEADLINE_SECONDS=$GRPC_DEADLINE_SECONDS"
    fi
    
    log "Executing: $mvn_cmd"
    
    # Run tests with timeout
    if timeout 1800 $mvn_cmd; then  # 30 minute timeout
        success "Tests completed successfully!"
        return 0
    else
        exit_code=$?
        if [[ $exit_code -eq 124 ]]; then
            error "Tests timed out after 30 minutes"
        else
            error "Tests failed with exit code: $exit_code"
        fi
        return $exit_code
    fi
}

# Post-test cleanup and reporting
post_test_cleanup() {
    log "Performing post-test cleanup..."
    
    # Copy test results to mounted volumes if they exist
    if [[ -d "/app/target/allure-results" && -d "target/allure-results" ]]; then
        cp -r target/allure-results/* /app/target/allure-results/ 2>/dev/null || true
    fi
    
    if [[ -d "/app/target/surefire-reports" && -d "target/surefire-reports" ]]; then
        cp -r target/surefire-reports/* /app/target/surefire-reports/ 2>/dev/null || true
    fi
    
    # Print test summary
    print_test_summary
    
    success "Post-test cleanup completed."
}

# Print test execution summary
print_test_summary() {
    log "=== Test Execution Summary ==="
    
    # Count test results
    if [[ -f "target/surefire-reports/testng-results.xml" ]]; then
        local total=$(grep -o 'total="[0-9]*"' target/surefire-reports/testng-results.xml | cut -d'"' -f2)
        local passed=$(grep -o 'passed="[0-9]*"' target/surefire-reports/testng-results.xml | cut -d'"' -f2)
        local failed=$(grep -o 'failed="[0-9]*"' target/surefire-reports/testng-results.xml | cut -d'"' -f2)
        local skipped=$(grep -o 'skipped="[0-9]*"' target/surefire-reports/testng-results.xml | cut -d'"' -f2)
        
        log "Total Tests: $total"
        log "Passed: $passed"
        log "Failed: $failed"
        log "Skipped: $skipped"
    else
        warning "TestNG results file not found"
    fi
    
    # List available reports
    log "Generated Reports:"
    if [[ -d "target/surefire-reports" ]]; then
        ls -la target/surefire-reports/ | grep -E '\.(xml|html)$' || log "No report files found"
    fi
    
    if [[ -d "target/allure-results" ]]; then
        local allure_count=$(find target/allure-results -name "*.json" | wc -l)
        log "Allure result files: $allure_count"
    fi
    
    log "=============================="
}

# Signal handlers for graceful shutdown
cleanup() {
    log "Received termination signal. Cleaning up..."
    post_test_cleanup
    exit 130
}

trap cleanup SIGINT SIGTERM

# Main execution flow
main() {
    log "Starting test execution script..."
    
    print_env_info
    validate_environment
    wait_for_dependencies
    pre_test_setup
    
    # Run tests
    if run_tests; then
        success "All tests completed successfully!"
        exit_code=0
    else
        error "Some tests failed!"
        exit_code=1
    fi
    
    post_test_cleanup
    
    log "Test execution script completed."
    exit $exit_code
}

# Help function
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Environment Variables:"
    echo "  TEST_SUITE          Test suite to run (default: smoke.xml)"
    echo "  ENV                 Test environment (default: dev)"
    echo "  PARALLEL_TESTS      Number of parallel test threads (default: 1)"
    echo "  RETRY_COUNT         Number of test retries (default: 2)"
    echo "  USER_SERVICE_HOST   gRPC service host"
    echo "  USER_SERVICE_PORT   gRPC service port"
    echo "  GRPC_DEADLINE_SECONDS gRPC request timeout"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run with defaults"
    echo "  TEST_SUITE=regression.xml $0         # Run regression tests"
    echo "  PARALLEL_TESTS=4 $0                  # Run with 4 parallel threads"
}

# Parse command line arguments
case "${1:-}" in
    -h|--help)
        show_help
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac