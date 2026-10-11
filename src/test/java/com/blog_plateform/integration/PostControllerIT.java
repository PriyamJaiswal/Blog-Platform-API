package com.blog_plateform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String registerAndGetToken(String username) throws Exception {
        String body = String.format(
                "{\"username\":\"%s\",\"email\":\"%s@test.com\",\"password\":\"pass1234\"}",
                username, username);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("token").asText();
    }

    @Test
    void shouldCreateAndFetchPost() throws Exception {
        String token = registerAndGetToken("ituser1");

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"IT Post\",\"content\":\"IT Content\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("IT Post"));
    }
}
