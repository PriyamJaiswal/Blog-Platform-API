package com.blog_plateform.service;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.dto.responseDto.PostResponse;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.CommentRepo;
import com.blog_plateform.repository.LikeRepo;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepo postRepository;
    private final UserRepo userRepository;
    private final LikeRepo likeRepository;
    private final CommentRepo commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    public Post createPost(PostRequest req, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setAuthor(author);
        Post saved = postRepository.save(post);
        evictFeedCache();
        return saved;
    }


    public Page<Post> getPostsOffset(int page, int size) {
        return postRepository.findAllWithAuthor(PageRequest.of(page, size));
    }

    // NEW — cached version
    @Cacheable(value = "postFeed", key = "'offset:' + #page + ':' + #size")
    public List<PostResponse> getPostsOffsetCached(int page, int size) {
        Page<Post> postsPage = getPostsOffset(page, size);
        return toResponseList(postsPage.getContent());
    }

    public List<Post> getPostsCursor(Long afterId, int limit) {
        if (afterId == null) {
            return postRepository.findTopWithAuthor(PageRequest.of(0, limit));
        }
        return postRepository.findByIdLessThanWithAuthor(afterId, PageRequest.of(0, limit));
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
    }

    @Cacheable(value = "postDetail", key = "#id")
    public PostResponse getPostByIdCached(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        return toResponse(post);
    }

    public long getTotalPostCount() {
        return postRepository.count();
    }

    // UPDATED — @CacheEvict + evictFeedCache() added
    @CacheEvict(value = "postDetail", key = "#id")
    public Post updatePost(Long id, PostRequest req, String username) {
        Post post = getPostById(id);
        checkOwnership(post, username);
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        Post updated = postRepository.save(post);
        evictFeedCache();
        return updated;
    }

    // UPDATED — @CacheEvict + evictFeedCache() added
    @CacheEvict(value = "postDetail", key = "#id")
    public void deletePost(Long id, String username) {
        Post post = getPostById(id);
        checkOwnership(post, username);
        postRepository.delete(post);
        evictFeedCache();
    }

    private void checkOwnership(Post post, String username) {
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Not your post");
        }
    }

    // NEW — private helper, class ke andar kahin bhi rakh sakte ho
    private void evictFeedCache() {
        Set<String> keys = redisTemplate.keys("feed::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        Cache postFeedCache = cacheManager.getCache("postFeed");
        if (postFeedCache != null) {
            postFeedCache.clear();
        }
    }

    //for single post
    public PostResponse toResponse(Post post) {
        long likes = likeRepository.countByPostId(post.getId());
        long comments = commentRepository.findByPostId(post.getId()).size();
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(),
                post.getAuthor().getUsername(), post.getCreatedAt(), likes, comments);
    }

    //for list
    public List<PostResponse> toResponseList(List<Post> posts) {
        if (posts.isEmpty()) return new ArrayList<>();   // List.of() -> ArrayList for consistency

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        Map<Long, Long> likeCounts = likeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, Long> commentCounts = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return posts.stream().map(post -> new PostResponse(
                post.getId(), post.getTitle(), post.getContent(),
                post.getAuthor().getUsername(), post.getCreatedAt(),
                likeCounts.getOrDefault(post.getId(), 0L),
                commentCounts.getOrDefault(post.getId(), 0L)
        )).collect(Collectors.toCollection(ArrayList::new));   //

    }
}