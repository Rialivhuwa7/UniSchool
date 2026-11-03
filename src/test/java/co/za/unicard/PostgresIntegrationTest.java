package co.za.unicard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against a real PostgreSQL (same major version as production) in Docker:
 * Flyway applies the migrations, Hibernate validates every entity against them
 * (ddl-auto=validate, exactly as in staging/prod), then the main flows run.
 * Skipped automatically when Docker isn't available.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.h2.console.enabled=false"
})
class PostgresIntegrationTest extends ApiTestSupport {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void schemaValidatesAndParentFlowsWork() throws Exception {
        String parent = login("naledi");
        var kids = body(get(parent, "/api/learners"));
        long thabo = find(kids, "firstName", "Thabo").get("id").asLong();

        // tuckshop: place an order for tomorrow-ish and check the wallet moved
        var week = body(get(parent, "/api/tuckshop/week"));
        String day = find(week, "name", "Thabo").get("days").get(2).get("day").asText();
        long wrap = menuId(parent, "Chicken wrap");
        putJson(parent, "/api/tuckshop/learners/" + thabo + "/orders",
                Map.of("day", day, "items", List.of(Map.of("menuItemId", wrap, "quantity", 2)), "payFrom", "PARENT"))
                .andExpect(status().isOk());
        assertThat(body(get(parent, "/api/wallet")).get("balance").decimalValue()).isEqualByComparingTo("2386.00");

        // school payments and fees
        var cols = body(get(parent, "/api/payments/collections"));
        long tour = find(cols, "title", "Grade 8 rugby tour, Durban").get("collectionId").asLong();
        postJson(parent, "/api/payments/collections/" + tour + "/pay", Map.of("learnerId", thabo, "part", "INSTALMENT"))
                .andExpect(status().isOk());
        get(parent, "/api/fees").andExpect(status().isOk());
    }

    @Test
    void staffViewsWork() throws Exception {
        get(login("khumalo"), "/api/principal/dashboard").andExpect(status().isOk());
        get(login("vanwyk"), "/api/school/overview").andExpect(status().isOk());
        get(login("vanwyk"), "/api/school/payouts").andExpect(status().isOk());
        get(login("naidoo"), "/api/teacher/class").andExpect(status().isOk());
        get(login("sithole"), "/api/pos/today").andExpect(status().isOk());
    }
}
