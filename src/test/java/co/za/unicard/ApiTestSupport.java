package co.za.unicard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.StreamSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for API tests. Boots the app with the demo data, signs in through the real
 * /api/auth/login endpoint and rolls back each test's changes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "unischool";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected String login(String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    protected ResultActions get(String token, String url) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions postJson(String token, String url, Object body) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body instanceof String s ? s : json.writeValueAsString(body)));
    }

    protected ResultActions putJson(String token, String url, Object body) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body instanceof String s ? s : json.writeValueAsString(body)));
    }

    protected ResultActions patchJson(String token, String url, Object body) throws Exception {
        return mvc.perform(patch(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body instanceof String s ? s : json.writeValueAsString(body)));
    }

    protected ResultActions delete(String token, String url) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url).header("Authorization", "Bearer " + token));
    }

    protected JsonNode body(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** First element of a JSON array whose field equals value. */
    protected JsonNode find(JsonNode array, String field, String value) {
        return StreamSupport.stream(array.spliterator(), false)
                .filter(n -> value.equals(n.path(field).asText()))
                .findFirst().orElseThrow(() -> new AssertionError("No element with " + field + "=" + value + " in " + array));
    }

    protected long menuId(String token, String name) throws Exception {
        return find(body(get(token, "/api/tuckshop/menu")), "name", name).get("id").asLong();
    }
}
