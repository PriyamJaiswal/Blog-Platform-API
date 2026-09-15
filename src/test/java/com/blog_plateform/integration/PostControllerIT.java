package com.blog_plateform.integration;

import com.blog_plateform.entity.User;
import com.blog_plateform.repository.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepo userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void setup() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);

        String loginBody = """
            {"username": "testuser", "password": "password123"}
            """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        token = objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void createPost_thenGetById_shouldReturnCreatedPost() throws Exception {
        String postBody = """
            {"title": "Integration Test Post", "content": "Testing full flow"}
            """;

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andReturn();

        Long postId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Test Post"));
    }

    @Test
    void createPost_withoutAuth_shouldReturn401() throws Exception {
        String postBody = """
            {"title": "Should fail", "content": "No auth"}
            """;

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePost_byNonOwner_shouldReturn403() throws Exception {
        // create post as testuser
        String postBody = """
            {"title": "Original", "content": "Original content"}
            """;

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody))
                .andExpect(status().isCreated())
                .andReturn();

        Long postId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // register second user
        String registerBody = """
            {"username": "intruder", "email": "intruder@test.com", "password": "password123"}
            """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        String intruderToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        String updateBody = """
            {"title": "Hacked", "content": "Hacked content"}
            """;

        mockMvc.perform(put("/api/posts/" + postId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFeed_cursorPagination_shouldReturnPostsInDescendingOrder() throws Exception {
        for (int i = 1; i <= 3; i++) {
            String body = String.format("""
                {"title": "Post %d", "content": "Content %d"}
                """, i, i);

            mockMvc.perform(post("/api/posts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/posts/feed?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Post 3"));
    }
}
