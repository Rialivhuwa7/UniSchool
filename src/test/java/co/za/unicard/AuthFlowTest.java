package co.za.unicard;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends ApiTestSupport {

    @Test
    void loginReturnsTokenAndActiveRole() throws Exception {
        String token = login("naledi");
        get(token, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Naledi Mokoena"))
                .andExpect(jsonPath("$.activeRole.role").value("PARENT"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"naledi\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiNeedsAToken() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/learners"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teacherCanSwitchToParentRole() throws Exception {
        String teacher = login("naidoo");
        get(teacher, "/api/auth/me").andExpect(jsonPath("$.activeRole.role").value("TEACHER"));
        // As a teacher she can't see the parent wallet...
        get(teacher, "/api/wallet").andExpect(status().isForbidden());

        String parent = body(postJson(teacher, "/api/auth/switch-role", "{\"role\":\"PARENT\"}")
                .andExpect(status().isOk())).get("token").asText();
        get(parent, "/api/wallet").andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(860.00));
        get(parent, "/api/learners").andExpect(jsonPath("$[0].firstName").value("Kayla"));
    }

    @Test
    void unverifiedStaffCannotSignInUntilApproved() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sibongile\",\"password\":\"unischool\"}"))
                .andExpect(status().isForbidden());

        String office = login("vanwyk");
        var pending = body(get(office, "/api/school/access/requests"));
        long id = find(pending, "username", "sibongile").get("id").asLong();
        // A teacher can only be approved once the office says what they teach.
        postJson(office, "/api/school/access/requests/" + id + "/approve", "{}").andExpect(status().isBadRequest());
        postJson(office, "/api/school/access/requests/" + id + "/approve", "{\"campus\":\"PREP\",\"grade\":\"Grade 5\",\"className\":\"5A\"}")
                .andExpect(status().isOk());

        String teacher = login("sibongile");
        get(teacher, "/api/auth/me").andExpect(jsonPath("$.activeRole.grade").value("Grade 5"))
                .andExpect(jsonPath("$.activeRole.className").value("5A"))
                .andExpect(jsonPath("$.activeRole.label").value("Grade 5 teacher, Highveld Ridge Prep"));
    }

    @Test
    void parentCanNameChildrenWhoArentInUniSchoolYet() throws Exception {
        register("{\"username\":\"newparent\",\"password\":\"password123\",\"fullName\":\"New Parent\",\"role\":\"PARENT\",\"children\":["
                + "{\"studentNumber\":\"2026105\",\"firstName\":\"Kian\",\"lastName\":\"Pillay\",\"grade\":\"Grade 9\",\"campus\":\"COLLEGE\"},"
                + "{\"firstName\":\"Mia\",\"lastName\":\"Pillay\",\"grade\":\"Grade 2\",\"campus\":\"PREP\"}]}");

        String office = login("vanwyk");
        var request = find(body(get(office, "/api/school/access/requests")), "username", "newparent");
        assertThat(request.get("learners").toString()).contains("Kian Pillay", "in UniSchool", "Mia Pillay", "new");
        postJson(office, "/api/school/access/requests/" + request.get("id").asLong() + "/approve", "{}").andExpect(status().isOk());

        String parent = signIn("newparent", "password123");
        var kids = body(get(parent, "/api/learners"));
        assertThat(kids).hasSize(2);
        assertThat(find(kids, "firstName", "Mia").get("studentNumber").asText()).startsWith("U");
    }

    @Test
    void learnersSignUpThemselves() throws Exception {
        register("{\"username\":\"sipho.m\",\"password\":\"password123\",\"fullName\":\"Sipho Mabaso\",\"role\":\"LEARNER\","
                + "\"learner\":{\"firstName\":\"Sipho\",\"lastName\":\"Mabaso\",\"grade\":\"Grade 10\",\"campus\":\"COLLEGE\"}}");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sipho.m\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());

        String office = login("vanwyk");
        long id = find(body(get(office, "/api/school/access/requests")), "username", "sipho.m").get("id").asLong();
        postJson(office, "/api/school/access/requests/" + id + "/approve", "{}").andExpect(status().isOk());

        String learner = signIn("sipho.m", "password123");
        get(learner, "/api/me/card").andExpect(jsonPath("$.learner.firstName").value("Sipho"))
                .andExpect(jsonPath("$.learner.grade").value("Grade 10"));
        get(learner, "/api/homework").andExpect(status().isOk());
        get(learner, "/api/tuckshop/week").andExpect(status().isOk());
    }

    @Test
    void aLearnerWhoAlreadyHasASignInCantGetASecond() throws Exception {
        register("{\"username\":\"lerato2\",\"password\":\"password123\",\"fullName\":\"Lerato Mokoena\",\"role\":\"LEARNER\","
                + "\"learner\":{\"studentNumber\":\"2026102\",\"firstName\":\"Lerato\",\"lastName\":\"Mokoena\",\"grade\":\"Grade 4\",\"campus\":\"PREP\"}}");
        String office = login("vanwyk");
        long id = find(body(get(office, "/api/school/access/requests")), "username", "lerato2").get("id").asLong();
        postJson(office, "/api/school/access/requests/" + id + "/approve", "{}").andExpect(status().isConflict());
    }

    @Test
    void staffSignUpNeedsNoCode() throws Exception {
        register("{\"username\":\"newtuck\",\"password\":\"password123\",\"fullName\":\"New Tuckshop\",\"role\":\"TUCKSHOP\"}");
    }

    private String signIn(String username, String password) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())).get("token").asText();
    }

    private void register(String json) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isAccepted());
    }
}
