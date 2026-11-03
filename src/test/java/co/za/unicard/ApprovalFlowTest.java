package co.za.unicard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teachers' notices reach parents only after the principal approves them. */
class ApprovalFlowTest extends ApiTestSupport {

    @Test
    void teacherNoticeGoesThroughPrincipal() throws Exception {
        String teacher = login("naidoo");
        var posted = body(postJson(teacher, "/api/staff/notices", Map.of(
                "title", "Library books due back", "body", "Please return library books by Friday.",
                "campus", "PREP", "grades", List.of("Grade 4"), "tag", "Reminder")).andExpect(status().isCreated()));
        assertThat(posted.get("status").asText()).isEqualTo("PENDING");
        long id = posted.get("id").asLong();

        String parent = login("naledi");
        assertThat(body(get(parent, "/api/notices")).findValuesAsText("title")).doesNotContain("Library books due back");

        String principal = login("khumalo");
        var queue = body(get(principal, "/api/principal/approvals"));
        assertThat(queue.findValuesAsText("title")).contains("Library books due back", "Grade 4 market day");

        postJson(principal, "/api/principal/approvals/notices/" + id + "/approve", "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        assertThat(body(get(parent, "/api/notices")).findValuesAsText("title")).contains("Library books due back");
    }

    @Test
    void principalCanSendBackForChanges() throws Exception {
        String principal = login("khumalo");
        long id = find(body(get(principal, "/api/principal/approvals")), "title", "Grade 4 market day").get("id").asLong();
        postJson(principal, "/api/principal/approvals/notices/" + id + "/request-changes", Map.of("comment", "Add the date please."))
                .andExpect(jsonPath("$.status").value("CHANGES_REQUESTED"));

        String teacher = login("naidoo");
        var mine = body(get(teacher, "/api/staff/notices/mine"));
        assertThat(find(mine, "title", "Grade 4 market day").get("reviewComment").asText()).isEqualTo("Add the date please.");
        putJson(teacher, "/api/staff/notices/" + id, Map.of("title", "Grade 4 market day, 7 November", "body", "Stalls cost R20."))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void homeworkPublishesDirectlyByDefault() throws Exception {
        String teacher = login("naidoo");
        var posted = body(postJson(teacher, "/api/teacher/homework", Map.of(
                "subject", "Maths", "title", "Times tables 6 to 9", "dueDate", java.time.LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isCreated()));
        assertThat(posted.get("status").asText()).isEqualTo("PUBLISHED");

        String learner = login("lerato");
        var hw = body(get(learner, "/api/homework"));
        long hwId = find(hw, "title", "Times tables 6 to 9").get("id").asLong();
        long me = hw.get(0).get("learnerId").asLong();
        putJson(learner, "/api/homework/" + hwId + "/done", Map.of("learnerId", me, "done", true))
                .andExpect(jsonPath("$.done").value(true));

        var cls = body(get(teacher, "/api/teacher/class"));
        assertThat(find(cls.get("learners"), "name", "Lerato M.").get("homeworkDone").asLong()).isEqualTo(1);
    }

    @Test
    void principalDashboardLoads() throws Exception {
        String principal = login("khumalo");
        var d = body(get(principal, "/api/principal/dashboard").andExpect(status().isOk()));
        assertThat(d.get("waitingForApproval").size()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("attention").findValuesAsText("area")).contains("fees", "uniform");
    }

    @Test
    void schoolOfficeCreatesCollectionAndExportsCsv() throws Exception {
        String office = login("vanwyk");
        postJson(office, "/api/school/collections", Map.of("title", "Grade 9 museum trip", "amount", 150, "kind", "PAYMENT",
                "campus", "COLLEGE", "grades", List.of("Grade 9"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.eligible").value(2));
        String csv = get(office, "/api/school/collections/export").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("Collection,Type").contains("Grade 9 museum trip");
    }
}
