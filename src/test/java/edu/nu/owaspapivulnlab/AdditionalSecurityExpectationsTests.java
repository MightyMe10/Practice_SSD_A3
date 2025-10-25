package edu.nu.owaspapivulnlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import edu.nu.owaspapivulnlab.service.RateLimiterService;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdditionalSecurityExpectationsTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired RateLimiterService rateLimiter;

    @BeforeEach
    void resetRateLimiter() {
        rateLimiter.reset();
    }

    String login(String user, String pw) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\""+user+"\",\"password\":\""+pw+"\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(res);
        return n.get("token").asText();
    }

    @Test
    void protected_endpoints_require_authentication() throws Exception {
        // Expectation in fixed app: /api/users requires auth -> 401
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized()); // Fails now due to permitAll on GET
    }

    @Test
    void delete_user_requires_admin() throws Exception {
        String tUser = login("alice", "alice123"); // not admin
        mvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + tUser))
                .andExpect(status().isForbidden()); // Fails now
    }

    @Test
    void create_user_does_not_allow_role_escalation() throws Exception {
        // In fixed app, server should ignore role/isAdmin from payload & return 201
        ObjectNode payload = om.createObjectNode()
                .put("username", "eve2")
                .put("password", "pw")
                .put("email", "e2@e")
                .put("role", "ADMIN")
                .put("isAdmin", true);
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload.toString()))
                .andExpect(status().isCreated()) // Fails now (200 OK)
                .andExpect(jsonPath("$.role", anyOf(nullValue(), is("USER")))) // Fails now (ADMIN)
                .andExpect(jsonPath("$.isAdmin", anyOf(nullValue(), is(false)))); // Fails now (true)
    }

    @Test
    void jwt_must_be_valid_and_aud_iss_checked() throws Exception {
    String valid = login("alice", "alice123");
    mvc.perform(get("/api/accounts/mine")
            .header("Authorization", "Bearer " + valid))
        .andExpect(status().isOk());

    String tampered = valid.substring(0, valid.length() - 2) + "aa";
    mvc.perform(get("/api/accounts/mine")
            .header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("invalid_token")));
    }

    @Test
    void account_owner_only_access() throws Exception {
    String alice = login("alice", "alice123");
        // In fixed code this should be forbidden
    mvc.perform(get("/api/accounts/2/balance")
            .header("Authorization", "Bearer " + alice))
                .andExpect(status().isForbidden()); // Fails now
    }

    @Test
    void login_attempts_are_rate_limited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice\",\"password\":\"bad\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"bad\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error", is("rate_limit_exceeded")));
    }

    @Test
    void transfers_are_rate_limited_per_user() throws Exception {
        String alice = login("alice", "alice123");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/accounts/1/transfer")
                            .header("Authorization", "Bearer " + alice)
                            .param("amount", "1"))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/accounts/1/transfer")
                        .header("Authorization", "Bearer " + alice)
                        .param("amount", "1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error", is("rate_limit_exceeded")));
    }
}
