package co.za.unicard;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Builds the database from the Flyway migrations (not from Hibernate) on H2 in PostgreSQL mode,
 * loads the demo data and exercises the main flows. A missing or misnamed column in the
 * migrations fails here, without needing Docker.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:migrationcheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.h2.console.enabled=false"
})
class MigrationOnH2Test extends ApiTestSupport {

    @Test
    void demoDataLoadsIntoMigratedSchemaAndFlowsWork() throws Exception {
        String parent = login("naledi");
        get(parent, "/api/home").andExpect(status().isOk());
        var kids = body(get(parent, "/api/learners"));
        long lerato = find(kids, "firstName", "Lerato").get("id").asLong();

        var catalog = body(get(parent, "/api/shop/catalog?learnerId=" + lerato));
        String key = find(catalog, "name", "Grey shorts").get("key").asText();
        postJson(parent, "/api/shop/cart", Map.of("learnerId", lerato, "key", key)).andExpect(status().isOk());
        postJson(parent, "/api/shop/checkout", Map.of("delivery", "TO_SCHOOL", "paymentMethod", "WALLET"))
                .andExpect(status().isCreated());

        get(parent, "/api/notices").andExpect(status().isOk());
        get(parent, "/api/payments/collections").andExpect(status().isOk());
        get(parent, "/api/savings").andExpect(status().isOk());

        String principal = login("khumalo");
        var dash = body(get(principal, "/api/principal/dashboard").andExpect(status().isOk()));
        assertThat(dash.get("waitingForApproval").size()).isGreaterThanOrEqualTo(1);

        String till = login("sithole");
        get(till, "/api/pos/today").andExpect(status().isOk());
    }
}
