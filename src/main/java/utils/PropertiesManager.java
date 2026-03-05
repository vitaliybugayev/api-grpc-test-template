package utils;

import org.testng.TestException;

import java.io.IOException;
import java.util.Properties;

public class PropertiesManager {

    public static final String ENV = System.getProperty("env") == null ? "dev" : System.getProperty("env");
    private static volatile Properties CACHED_PROPERTIES;

    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return getProperties().getProperty(key, defaultValue);
    }

    private static Properties getProperties() {
        if (CACHED_PROPERTIES == null) {
            synchronized (ProjectConfiguration.class) {
                if (CACHED_PROPERTIES == null) {
                    CACHED_PROPERTIES = loadProperties();
                }
            }
        }
        return CACHED_PROPERTIES;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        String fileName = String.format("envs/%s.properties", ENV);
        try (InputStream is = ProjectConfiguration.class
                .getClassLoader()
                .getResourceAsStream(fileName)) {
            Objects.requireNonNull(is, "Properties file not found: " + fileName);
            properties.load(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties: " + fileName, e);
        }
        return properties;
    }
}
