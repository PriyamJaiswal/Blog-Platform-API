package com.blog_plateform.integration;

import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import com.blog_plateform.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PostService postService;
    @Autowired private PostRepo postRepo;
    @Autowired private UserRepo userRepo;

    @Test
    void getPostById_shouldReturnCachedValueOnSecondCall() {
        User user = new User();
        user.setUsername("cacheuser1");
        user.setEmail("cache1@test.com");
        user.setPassword("pass");
        userRepo.save(user);

        Post post = new Post();
        post.setTitle("Cache Test");
        post.setContent("Content");
        post.setAuthor(user);
        postRepo.save(post);

        var first = postService.getPostResponseById(post.getId());
        var second = postService.getPostResponseById(post.getId());

        assertThat(first.getTitle()).isEqualTo(second.getTitle());
    }
}
