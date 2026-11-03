package co.za.unicard;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staging and production start with an empty database and no demo data. This walks the whole
 * set-up as real people would: the first office account comes from configuration, the office
 * sets the school up, and staff and parents ask for access and use the app.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:emptyschool;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "unischool.seed-demo=false",
        "unischool.admin.username=office",
        "unischool.admin.password=first-office-password",
        "unischool.admin.full-name=Ms Office"
})
class EmptySchoolJourneyTest extends ApiTestSupport {

    private static final String OFFICE_PASSWORD = "first-office-password";

    @Test
    void aNewSchoolIsSetUpAndUsedWithoutDemoData() throws Exception {
        // The only account is the configured school office.
        String office = signIn("office", OFFICE_PASSWORD);
        assertThat(body(get(office, "/api/school/learners"))).isEmpty();
        get(office, "/api/school/overview").andExpect(status().isOk());

        // The office enrols a learner and sets the school up.
        long learnerId = body(postJson(office, "/api/school/learners", Map.of(
                "studentNumber", "2027001", "firstName", "Naledi", "lastName", "Dube", "grade", "Grade 4",
                "campus", "PREP", "house", "Kudu", "sports", List.of("Hockey")))
                .andExpect(status().isCreated())).get("id").asLong();
        postJson(office, "/api/school/learners", Map.of(
                "studentNumber", "2027001", "firstName", "Someone", "lastName", "Else", "grade", "Grade 4", "campus", "PREP"))
                .andExpect(status().isConflict());

        postJson(office, "/api/school/fees/load", Map.of("campus", "PREP", "grade", "Grade 4", "annualFee", 48000))
                .andExpect(status().isOk());
        get(office, "/api/school/fees/schedule").andExpect(jsonPath("$.grades[0].loaded").value(1))
                .andExpect(jsonPath("$.grades[0].annualFee").value(48000.00));

        postJson(office, "/api/school/uniform/items", Map.of("campus", "PREP", "name", "Grey shorts", "price", 179,
                "kind", "GENERIC", "sizeType", "Shorts")).andExpect(status().isCreated());
        postJson(office, "/api/school/uniform/stock", Map.of("itemName", "Grey shorts", "size", "9-10", "quantity", 20))
                .andExpect(status().isNoContent());
        get(office, "/api/school/uniform").andExpect(jsonPath("$.stock[0].quantity").value(20));
        postJson(office, "/api/school/sport", Map.of("sport", "Hockey", "type", "BUY", "name", "Hockey stick", "price", 650))
                .andExpect(status().isCreated());
        postJson(office, "/api/school/groups", Map.of("name", "Grade 4 parents", "campus", "PREP", "grade", "Grade 4", "mode", "OPEN"))
                .andExpect(status().isCreated());
        putJson(office, "/api/school/lists/PREP/Grade 4", Map.of("publish", true,
                "items", List.of(Map.of("name", "HB pencils, 12", "quantity", 1, "unitPrice", 45))))
                .andExpect(status().isOk());
        get(office, "/api/school/lists/PREP/Grade 4").andExpect(jsonPath("$.items[0].name").value("HB pencils, 12"));

        // A parent asks for access with the child's student number; the office approves.
        register(Map.of("username", "parent1", "password", "parent-password", "fullName", "Thandi Dube",
                "role", "PARENT", "children", List.of(
                Map.of("studentNumber", "2027001", "firstName", "Naledi", "lastName", "Dube", "grade", "Grade 4", "campus", "PREP"))));
        approve(office, "parent1", null);
        String parent = signIn("parent1", "parent-password");
        get(parent, "/api/learners").andExpect(jsonPath("$[0].firstName").value("Naledi"));
        get(parent, "/api/fees").andExpect(jsonPath("$[0].annualFee").value(48000.00));
        get(parent, "/api/home").andExpect(status().isOk());
        get(parent, "/api/groups").andExpect(jsonPath("$[0].name").value("Grade 4 parents"));

        // The parent makes the child's sign-in and puts money on the card.
        get(parent, "/api/learners/" + learnerId + "/sign-in").andExpect(jsonPath("$.username").doesNotExist());
        postJson(parent, "/api/learners/" + learnerId + "/sign-in", Map.of("username", "naledi.d", "password", "learner-password"))
                .andExpect(status().isCreated());
        postJson(parent, "/api/learners/" + learnerId + "/sign-in", Map.of("username", "naledi.d2", "password", "learner-password"))
                .andExpect(status().isConflict());
        postJson(parent, "/api/wallet/top-up", Map.of("amount", 200)).andExpect(status().isOk());
        postJson(parent, "/api/wallet/move/" + learnerId, Map.of("amount", 100)).andExpect(status().isOk());
        patchJson(parent, "/api/learners/" + learnerId + "/controls", Map.of("tuckMode", "FREE_CHOICE", "dailyLimit", 60))
                .andExpect(status().isOk());

        String learner = signIn("naledi.d", "learner-password");
        get(learner, "/api/me/card").andExpect(jsonPath("$.learner.walletBalance").value(100.00));

        // A learner the school hasn't enrolled yet signs up themselves; approval enrols them.
        register(Map.of("username", "lindo", "password", "lindo-password", "fullName", "Lindo Zulu", "role", "LEARNER",
                "learner", Map.of("firstName", "Lindo", "lastName", "Zulu", "grade", "Grade 4", "campus", "PREP")));
        approve(office, "lindo", null);
        get(signIn("lindo", "lindo-password"), "/api/me/card").andExpect(jsonPath("$.learner.firstName").value("Lindo"));

        // A teacher asks for access; the office says what they teach.
        register(Map.of("username", "teacher1", "password", "teacher-password", "fullName", "Sipho Nkosi",
                "role", "TEACHER", "ask", "Grade 4 teacher"));
        approve(office, "teacher1", Map.of("campus", "PREP", "grade", "Grade 4", "className", "4A"));
        String teacher = signIn("teacher1", "teacher-password");
        assertThat(body(get(teacher, "/api/teacher/class")).get("learners").toString()).contains("Naledi D.", "Lindo Z.");

        // The tuckshop sets up its menu and sells to the learner.
        register(Map.of("username", "tuck1", "password", "tuck-password", "fullName", "Grace Mokoena",
                "role", "TUCKSHOP"));
        approve(office, "tuck1", null);
        String tuckshop = signIn("tuck1", "tuck-password");
        long pie = body(postJson(tuckshop, "/api/pos/menu", Map.of("name", "Mince pie", "price", 25, "category", "Hot food"))
                .andExpect(status().isCreated())).get("id").asLong();
        var sale = body(postJson(tuckshop, "/api/pos/charge", Map.of("learnerId", learnerId, "menuItemIds", List.of(pie)))
                .andExpect(status().isOk()));
        assertThat(sale.get("approved").asBoolean()).isTrue();
        patchJson(tuckshop, "/api/pos/menu/" + pie, Map.of("available", false)).andExpect(status().isOk());
        get(parent, "/api/tuckshop/menu").andExpect(jsonPath("$").isEmpty());

        // A principal joins and sees the dashboard of the new school.
        register(Map.of("username", "principal1", "password", "principal-password", "fullName", "Dr Molefe",
                "role", "PRINCIPAL"));
        approve(office, "principal1", null);
        get(signIn("principal1", "principal-password"), "/api/principal/dashboard").andExpect(status().isOk());
    }

    @Test
    void theFirstAdminIsOnlyCreatedOnce() throws Exception {
        String office = signIn("office", OFFICE_PASSWORD);
        get(office, "/api/school/access/staff").andExpect(jsonPath("$.length()").value(1));
    }

    private String signIn(String username, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    private void register(Map<String, Object> body) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isAccepted());
    }

    private void approve(String office, String username, Map<String, Object> teaching) throws Exception {
        long id = find(body(get(office, "/api/school/access/requests")), "username", username).get("id").asLong();
        postJson(office, "/api/school/access/requests/" + id + "/approve", teaching == null ? "{}" : teaching)
                .andExpect(status().isOk());
    }
}
