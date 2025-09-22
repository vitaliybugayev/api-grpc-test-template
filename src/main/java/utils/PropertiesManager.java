package utils;

import org.testng.TestException;

import java.io.IOException;
import java.util.Properties;

public abstract class PropertiesManager {

    public static final String ENV = System.getProperty("env") == null ? "dev" : System.getProperty("env");
    private static volatile Properties CACHED_PROPERTIES;

    public static String getProperty(String name) {
        return getProperties().getProperty(name);
    }

    public static String getProperty(String name, String defaultValue) {
        return getProperties().getProperty(name, defaultValue);
    }

    public static int getIntProperty(String name, int defaultValue) {
        String value = getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLongProperty(String name, long defaultValue) {
        String value = getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static Properties getProperties() {
        var cached = CACHED_PROPERTIES;
        if (cached != null) return cached;
        synchronized (PropertiesManager.class) {
            if (CACHED_PROPERTIES == null) {
                CACHED_PROPERTIES = loadPropertiesForEnv(ENV);
            }
            return CACHED_PROPERTIES;
        }
    }

    private static Properties loadPropertiesForEnv(String env) {
        var props = new Properties();
        
        // Load test.properties first (base properties)
        try (var testStream = PropertiesManager.class.getResourceAsStream("/test.properties")) {
            if (testStream != null) {
                props.load(testStream);
            }
        } catch (IOException e) {
            // test.properties is optional, continue without it
        }
        
        // Load environment-specific properties (override base properties)
        var envPath = String.format("/envs/%s.properties", env);
        try (var envStream = PropertiesManager.class.getResourceAsStream(envPath)) {
            if (envStream == null) {
                throw new TestException(String.format("Environment file not found: %s", envPath));
            }
            props.load(envStream); // This will override properties from test.properties
            return props;
        } catch (IOException e) {
            throw new TestException("Failed to load environment properties: " + envPath, e);
        }
    }
}
