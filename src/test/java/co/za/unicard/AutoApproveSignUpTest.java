package co.za.unicard;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Local testing: with auto-approve on, anyone can sign up as any role and sign in straight away. */
@TestPropertySource(properties = "unischool.auto-approve-signups=true")
class AutoApproveSignUpTest extends ApiTestSupport {

    @Test
    void aTeacherSignsUpAndSignsInStraightAway() throws Exception {
        register("{\"username\":\"quick.teacher\",\"password\":\"password123\",\"fullName\":\"Quick Teacher\",\"role\":\"TEACHER\","
                + "\"teaching\":{\"campus\":\"PREP\",\"grade\":\"Grade 4\",\"className\":\"4C\"}}");
        get(signIn("quick.teacher"), "/api/teacher/class").andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value("Grade 4"));
    }

    @Test
    void aLearnerSignsUpAndSignsInStraightAway() throws Exception {
        register("{\"username\":\"quick.learner\",\"password\":\"password123\",\"fullName\":\"Quick Learner\",\"role\":\"LEARNER\","
                + "\"learner\":{\"firstName\":\"Quick\",\"lastName\":\"Learner\",\"grade\":\"Grade 6\",\"campus\":\"PREP\"}}");
        get(signIn("quick.learner"), "/api/me/card").andExpect(jsonPath("$.learner.grade").value("Grade 6"));
    }

    @Test
    void aTeacherWithoutAGradeIsTold() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no.grade\",\"password\":\"password123\",\"fullName\":\"No Grade\",\"role\":\"TEACHER\"}"))
                .andExpect(status().isBadRequest());
    }

    private void register(String json) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Your account is ready. You can sign in now."));
    }

    private String signIn(String username) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())).get("token").asText();
    }
}
