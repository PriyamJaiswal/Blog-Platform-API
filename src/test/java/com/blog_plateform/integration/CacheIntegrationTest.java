package com.blog_plateform.integration;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.dto.responseDto.PostResponse;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import com.blog_plateform.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class CacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PostService postService;
    @Autowired private PostRepo postRepository;
    @Autowired private UserRepo userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User author;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setUsername("cacheuser");
        user.setEmail("cache@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        author = userRepository.save(user);
    }

    @Test
    void getPostByIdCached_shouldServeFromCacheOnSecondCall() {
        Post post = new Post();
        post.setTitle("Cache Test");
        post.setContent("Content");
        post.setAuthor(author);
        Post saved = postRepository.save(post);

        // first call -> DB hit, populates cache
        PostResponse first = postService.getPostByIdCached(saved.getId());
        assertThat(first.getTitle()).isEqualTo("Cache Test");

        // directly change DB, bypassing the service/cache
        saved.setTitle("Changed Directly In DB");
        postRepository.save(saved);

        // second call should still return the OLD (cached) title
        PostResponse second = postService.getPostByIdCached(saved.getId());
        assertThat(second.getTitle()).isEqualTo("Cache Test");
    }

    @Test
    void updatePost_shouldEvictCacheAndReturnFreshData() {
        Post post = new Post();
        post.setTitle("Before Update");
        post.setContent("Content");
        post.setAuthor(author);
        Post saved = postRepository.save(post);

        // populate cache
        PostResponse cached = postService.getPostByIdCached(saved.getId());
        assertThat(cached.getTitle()).isEqualTo("Before Update");

        // update through the service -> should evict cache
        PostRequest req = new PostRequest();
        req.setTitle("After Update");
        req.setContent("New content");
        postService.updatePost(saved.getId(), req, "cacheuser");

        // next cached call should reflect the NEW title, not stale cache
        PostResponse afterUpdate = postService.getPostByIdCached(saved.getId());
        assertThat(afterUpdate.getTitle()).isEqualTo("After Update");
    }
}
