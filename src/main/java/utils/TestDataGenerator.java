package utils;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataGenerator {
    private static final String[] FIRST_NAMES = {"John", "Jane", "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank"};
    private static final String[] LAST_NAMES = {"Smith", "Johnson", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor"};
    private static final String[] DOMAINS = {"example.com", "test.com", "demo.org", "sample.net"};
    
    // Age constants
    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 80;
    private static final int AGE_RANGE = MAX_AGE - MIN_AGE;

    // Optional deterministic seed: -Drand.seed=1234
    // If not set, falls back to ThreadLocalRandom
    private static final Long RAND_SEED = Long.getLong("rand.seed");
    private static final java.util.Random SEEDED_RANDOM = (RAND_SEED != null) ? new java.util.Random(RAND_SEED) : null;

    // Optional namespacing: -Dnamespaced=true and optional -Drun.id=custom
    private static final boolean NAMESPACED = Boolean.parseBoolean(System.getProperty("namespaced", System.getenv().getOrDefault("NAMESPACED", "false")));
    private static final String RUN_ID = System.getProperty("run.id", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

    public static String randomId() {
        // UUID is sufficiently unique; keep as-is (not seeded)
        return UUID.randomUUID().toString();
    }

    public static String randomName() {
        return randomFrom(FIRST_NAMES) + " " + randomFrom(LAST_NAMES);
    }

    public static String randomEmail() {
        var username = randomFrom(FIRST_NAMES).toLowerCase() + "." + randomFrom(LAST_NAMES).toLowerCase();
        if (NAMESPACED) {
            username = username + "." + RUN_ID;
        }
        return username + "@" + randomFrom(DOMAINS);
    }

    public static Integer randomAge() {
        return MIN_AGE + nextInt(AGE_RANGE);
    }

    private static String randomFrom(String[] array) {
        int idx = nextInt(array.length);
        return array[idx];
    }

    private static int nextInt(int bound) {
        if (SEEDED_RANDOM != null) {
            synchronized (SEEDED_RANDOM) {
                return SEEDED_RANDOM.nextInt(bound);
            }
        }
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
