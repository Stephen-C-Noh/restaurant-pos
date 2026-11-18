package com.restaurant.pos.auth;

import com.restaurant.pos.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegrationTest {

    @Test
    void kitchenUser_logsInWithPin7890() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"7890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("kitchen1"))
                .andExpect(jsonPath("$.role").value("KITCHEN"));
    }
}
