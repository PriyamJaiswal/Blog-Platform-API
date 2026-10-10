package com.blog_plateform.unit;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.CommentRepo;
import com.blog_plateform.repository.LikeRepo;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import com.blog_plateform.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void createPost_shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        PostRequest req = new PostRequest();
        req.setTitle("Title");
        req.setContent("Content");

        assertThatThrownBy(() -> postService.createPost(req, "ghost"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void updatePost_shouldThrowWhenNotOwner() {
        User owner = new User();
        owner.setUsername("actualOwner");
        Post post = new Post();
        post.setId(1L);
        post.setAuthor(owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        PostRequest req = new PostRequest();
        req.setTitle("Hacked title");
        req.setContent("Hacked content");

        assertThatThrownBy(() -> postService.updatePost(1L, req, "someoneElse"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updatePost_shouldSucceedWhenOwnerMatches() {
        User owner = new User();
        owner.setUsername("priyam");
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Old Title");
        post.setAuthor(owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setTitle("New Title");
        req.setContent("New Content");

        Post updated = postService.updatePost(1L, req, "priyam");

        assertThat(updated.getTitle()).isEqualTo("New Title");
    }

    @Test
    void getPostById_shouldThrowWhenNotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deletePost_shouldThrowWhenNotOwner() {
        User owner = new User();
        owner.setUsername("actualOwner");
        Post post = new Post();
        post.setId(1L);
        post.setAuthor(owner);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(1L, "intruder"))
                .isInstanceOf(AccessDeniedException.class);
    }
}