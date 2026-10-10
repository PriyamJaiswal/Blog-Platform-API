package com.blog_plateform.unit;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import com.blog_plateform.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import com.blog_plateform.repository.CommentRepo;
import com.blog_plateform.repository.LikeRepo;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepo postRepository;
    @Mock private UserRepo userRepository;
    @Mock private LikeRepo likeRepository;
    @Mock private CommentRepo commentRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private CacheManager cacheManager;

    @InjectMocks private PostService postService;

    @Test
    void createPost_shouldSaveWithAuthor() {
        User user = new User();
        user.setUsername("priyam");
        when(userRepository.findByUsername("priyam")).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setTitle("Test Post");
        req.setContent("Test Content");

        Post result = postService.createPost(req, "priyam");

        assertThat(result.getTitle()).isEqualTo("Test Post");
        assertThat(result.getAuthor().getUsername()).isEqualTo("priyam");
        verify(postRepository).save(any(Post.class));
    }
}
