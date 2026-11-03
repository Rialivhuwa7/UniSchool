package co.za.unicard;

import org.junit.jupiter.api.Test;

/** Shares ApiTestSupport's configuration so all dev-profile tests reuse one application context. */
class UniSchoolTests extends ApiTestSupport {

    @Test
    void contextLoads() {
        // Fails if the application context (security, JPA mappings, demo seeder) can't start
    }
}
