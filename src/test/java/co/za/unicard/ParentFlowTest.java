package co.za.unicard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParentFlowTest extends ApiTestSupport {

    private String naledi;
    private long lerato;
    private long thabo;

    @BeforeEach
    void signIn() throws Exception {
        naledi = login("naledi");
        var kids = body(get(naledi, "/api/learners"));
        lerato = find(kids, "firstName", "Lerato").get("id").asLong();
        thabo = find(kids, "firstName", "Thabo").get("id").asLong();
    }

    @Test
    void moveMoneyToAChild() throws Exception {
        postJson(naledi, "/api/wallet/move/" + lerato, Map.of("amount", 100))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(2350.00));
        get(naledi, "/api/learners/" + lerato).andExpect(jsonPath("$.walletBalance").value(280.00));
    }

    @Test
    void cannotSeeSomeoneElsesChild() throws Exception {
        long kayla = body(get(login("sithole"), "/api/pos/learners?q=Kayla")).get(0).get("id").asLong();
        get(naledi, "/api/learners/" + kayla).andExpect(status().isForbidden());
    }

    @Test
    void cardControlsChangeAndValidate() throws Exception {
        patchJson(naledi, "/api/learners/" + thabo + "/controls", Map.of("dailyLimit", 100, "frozen", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(100.00))
                .andExpect(jsonPath("$.frozen").value(true));
        patchJson(naledi, "/api/learners/" + thabo + "/controls", Map.of("blockedCategories", List.of("Vegetables")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replacingAPreOrderRefundsTheOldOne() throws Exception {
        var week = body(get(naledi, "/api/tuckshop/week"));
        var thaboWeek = find(week, "name", "Thabo");
        String day = thaboWeek.get("days").get(0).get("day").asText();   // has a R41 parent-paid order
        long wrap = menuId(naledi, "Chicken wrap");

        var placed = body(putJson(naledi, "/api/tuckshop/learners/" + thabo + "/orders",
                Map.of("day", day, "items", List.of(Map.of("menuItemId", wrap, "quantity", 1)), "payFrom", "PARENT"))
                .andExpect(status().isOk()));
        assertThat(placed.get("total").decimalValue()).isEqualByComparingTo("32.00");
        // R2450 + R41 refund - R32 = R2459
        get(naledi, "/api/wallet").andExpect(jsonPath("$.balance").value(2459.00));

        delete(naledi, "/api/tuckshop/learners/" + thabo + "/orders/" + day).andExpect(status().isNoContent());
        get(naledi, "/api/wallet").andExpect(jsonPath("$.balance").value(2491.00));
    }

    @Test
    void shopCheckoutWithWalletGivesBackToSchool() throws Exception {
        var catalog = body(get(naledi, "/api/shop/catalog?learnerId=" + lerato + "&category=UNIFORM"));
        var blazer = find(catalog, "name", "School blazer with crest");
        assertThat(blazer.get("size").asText()).isEqualTo("30");

        postJson(naledi, "/api/shop/cart", Map.of("learnerId", lerato, "key", blazer.get("key").asText())).andExpect(status().isOk());
        postJson(naledi, "/api/shop/cart", Map.of("learnerId", lerato, "key", blazer.get("key").asText())).andExpect(status().isConflict());
        postJson(naledi, "/api/shop/cart/supply-list", Map.of("learnerId", lerato)).andExpect(status().isOk());

        var order = body(postJson(naledi, "/api/shop/checkout", Map.of("delivery", "TO_SCHOOL", "paymentMethod", "WALLET"))
                .andExpect(status().isCreated()));
        assertThat(order.get("orderNumber").asText()).startsWith("US-");
        assertThat(order.get("schoolGiveBack").decimalValue()).isPositive();
        get(naledi, "/api/shop/cart").andExpect(jsonPath("$.count").value(0));

        var home = body(get(naledi, "/api/home"));
        assertThat(find(home.get("readiness"), "area", "uniform").get("done").asBoolean()).isTrue();
    }

    @Test
    void preLovedItemCanOnlyBeSoldOnce() throws Exception {
        var catalog = body(get(naledi, "/api/shop/catalog?learnerId=" + lerato + "&category=PRELOVED"));
        var blazer = find(catalog, "name", "Prep blazer, size 30");
        assertThat(blazer.get("fits").asBoolean()).isTrue();
        postJson(naledi, "/api/shop/cart", Map.of("learnerId", lerato, "key", blazer.get("key").asText())).andExpect(status().isOk());
        postJson(naledi, "/api/shop/checkout", Map.of("delivery", "TO_HOME", "paymentMethod", "CARD")).andExpect(status().isCreated());

        var after = body(get(naledi, "/api/shop/catalog?learnerId=" + lerato + "&category=PRELOVED"));
        assertThat(after.findValuesAsText("name")).doesNotContain("Prep blazer, size 30");
    }

    @Test
    void payCampInInstalmentsAndBuyTickets() throws Exception {
        var cols = body(get(naledi, "/api/payments/collections"));
        var camp = find(cols, "title", "Grade 4 camp, Magaliesberg");
        long campId = camp.get("collectionId").asLong();

        var receipt = body(postJson(naledi, "/api/payments/collections/" + campId + "/pay",
                Map.of("learnerId", lerato, "part", "INSTALMENT")).andExpect(status().isOk()));
        assertThat(receipt.get("amountPaid").decimalValue()).isEqualByComparingTo("616.67");

        long concert = find(cols, "title", "Year-end concert tickets").get("collectionId").asLong();
        var tickets = body(postJson(naledi, "/api/payments/collections/" + concert + "/pay",
                Map.of("learnerId", lerato, "quantity", 2)).andExpect(status().isOk()));
        assertThat(tickets.get("ticketCodes")).hasSize(2);
        get(naledi, "/api/payments/tickets").andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void choosingAFeePlan() throws Exception {
        var fees = body(get(naledi, "/api/fees"));
        assertThat(find(fees, "learnerName", "Lerato").get("plan").isNull()).isTrue();
        putJson(naledi, "/api/fees/" + lerato + "/plan", Map.of("plan", "MONTHLY_10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eachPayment").value(9600.00));
    }

    @Test
    void acknowledgeNoticeForAChild() throws Exception {
        var notices = body(get(naledi, "/api/notices"));
        var camp = find(notices, "title", "Magaliesberg camp: kit list and consent");
        assertThat(camp.get("read").asBoolean()).isFalse();

        var after = body(postJson(naledi, "/api/notices/" + camp.get("id").asLong() + "/acknowledge", Map.of("learnerId", lerato))
                .andExpect(status().isOk()));
        assertThat(after.get("read").asBoolean()).isTrue();
        assertThat(after.get("children").get(0).get("acknowledged").asBoolean()).isTrue();
    }

    @Test
    void savingsPotProjection() throws Exception {
        var pot = body(get(naledi, "/api/savings"));
        assertThat(pot.get("balance").decimalValue()).isEqualByComparingTo("7150.00");
        var more = body(postJson(naledi, "/api/savings/add", Map.of("amount", 500)).andExpect(status().isOk()));
        assertThat(more.get("balance").decimalValue()).isEqualByComparingTo("7650.00");
        get(naledi, "/api/wallet").andExpect(jsonPath("$.balance").value(1950.00));
    }

    @Test
    void learnerSavesTowardsGoalOnlyWithAPositiveAmount() throws Exception {
        String learner = login("lerato");
        // A negative amount used to credit the card wallet and shrink the goal.
        postJson(learner, "/api/me/goal/save?amount=-50", "").andExpect(status().isBadRequest());
        postJson(learner, "/api/me/goal/save?amount=0", "").andExpect(status().isBadRequest());
        get(learner, "/api/me/card").andExpect(jsonPath("$.learner.walletBalance").value(180.00))
                .andExpect(jsonPath("$.learner.goal.saved").value(120.00));

        postJson(learner, "/api/me/goal/save?amount=20", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.walletBalance").value(160.00))
                .andExpect(jsonPath("$.goal.saved").value(140.00));
    }
}
