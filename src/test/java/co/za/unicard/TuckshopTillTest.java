package co.za.unicard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The till applies each parent's rules in order and never charges a declined sale. */
class TuckshopTillTest extends ApiTestSupport {

    private String till;

    @BeforeEach
    void signIn() throws Exception {
        till = login("sithole");
    }

    private long learner(String name) throws Exception {
        return body(get(till, "/api/pos/learners?q=" + name)).get(0).get("id").asLong();
    }

    private com.fasterxml.jackson.databind.JsonNode charge(String learner, String... items) throws Exception {
        List<Long> ids = new java.util.ArrayList<>();
        for (String i : items) ids.add(menuId(till, i));
        return body(postJson(till, "/api/pos/charge", Map.of("learnerId", learner(learner), "menuItemIds", ids))
                .andExpect(status().isOk()));
    }

    @Test
    void frozenCardIsDeclined() throws Exception {
        var r = charge("Sipho", "Water 500 ml");
        assertThat(r.get("approved").asBoolean()).isFalse();
        assertThat(r.get("message").asText()).contains("frozen");
    }

    @Test
    void preOrderOnlyLearnerCannotBuyExtras() throws Exception {
        var r = charge("Lerato", "Muffin");
        assertThat(r.get("approved").asBoolean()).isFalse();
        assertThat(r.get("message").asText()).contains("pre-orders");
    }

    @Test
    void approvedItemsModeRejectsOtherItems() throws Exception {
        var r = charge("Thabo", "Chips");
        assertThat(r.get("approved").asBoolean()).isFalse();
        assertThat(r.get("message").asText()).contains("approved list");
        assertThat(charge("Thabo", "Muffin").get("approved").asBoolean()).isTrue();
    }

    @Test
    void blockedCategoryIsDeclined() throws Exception {
        var r = charge("Kayla", "Fizzy drink");
        assertThat(r.get("approved").asBoolean()).isFalse();
        assertThat(r.get("message").asText()).contains("Fizzy drinks are blocked");
    }

    @Test
    void dailyLimitAndBalanceAreEnforced() throws Exception {
        // Amara: R64 wallet, R50 daily limit, R15 already spent today
        var ok = charge("Amara", "Chicken wrap");
        assertThat(ok.get("approved").asBoolean()).isTrue();
        assertThat(ok.get("walletBalance").decimalValue()).isEqualByComparingTo("32.00");

        var over = charge("Amara", "Muffin");
        assertThat(over.get("approved").asBoolean()).isFalse();
        assertThat(over.get("message").asText()).contains("daily limit");

        // Kian: R12 in the wallet
        var broke = charge("Kian", "Mince pie");
        assertThat(broke.get("approved").asBoolean()).isFalse();
        assertThat(broke.get("message").asText()).contains("Only R12.00");
    }

    @Test
    void preOrderCanBeHandedOverInParts() throws Exception {
        var today = body(get(till, "/api/pos/today"));
        var order = find(today.get("orders"), "learnerName", "Lerato");
        long id = order.get("id").asLong();

        var first = body(postJson(till, "/api/pos/pre-orders/" + id + "/handout", "{\"lineIndexes\":[0]}"));
        assertThat(first.get("ok").asBoolean()).isTrue();
        assertThat(first.at("/order/status").asText()).isEqualTo("PARTIAL");

        var rest = body(postJson(till, "/api/pos/pre-orders/" + id + "/handout", "{}"));
        assertThat(rest.at("/order/status").asText()).isEqualTo("COLLECTED");
    }

    @Test
    void parentsCannotUseTheTill() throws Exception {
        get(login("naledi"), "/api/pos/today").andExpect(status().isForbidden());
    }
}
