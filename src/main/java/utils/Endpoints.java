package utils;

public final class Endpoints {

    private Endpoints() {
        throw new IllegalStateException("Utility class");
    }

    private static final String VERSION = System.getProperty("version") == null ? "v1" : System.getProperty("version");

    // Health endpoints
    public static final String HEALTH_CHECK = String.format("/%s/health", VERSION);
    
    // User endpoints
    public static final String USERS = String.format("/%s/users", VERSION);
    public static final String USER_BY_ID = String.format("/%s/users/{id}", VERSION);
    
    // Add your specific endpoints here as needed
}
