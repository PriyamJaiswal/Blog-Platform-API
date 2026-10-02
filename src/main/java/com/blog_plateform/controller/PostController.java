package com.blog_plateform.controller;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.dto.responseDto.PostResponse;
import com.blog_plateform.entity.Post;
import com.blog_plateform.service.CachedFeedService;
import com.blog_plateform.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CachedFeedService cachedFeedService;

    @Operation(summary = "Create a new post", description = "Creates a new post with the provided details.")
    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest req, Authentication auth) {
        Post post = postService.createPost(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.toResponse(post));
    }

    // Offset pagination: /api/posts?page=0&size=10
    @Operation(summary = "Get all posts", description = "Retrieves all posts with offset and size parameters.")
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllOffset(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostsOffsetCached(page, size));
    }

    @Operation(summary = "Get feed", description = "Retrieves the feed with cursor-based pagination.")
    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeedCursor(
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                cachedFeedService.getFeed(after, limit)
        );
    }

    @Operation(summary = "Get a post by ID", description = "Retrieves a post by its ID.")
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(
                postService.getPostByIdCached(id)
        );
    }

    @Operation(summary = "Get total post count", description = "Retrieves the total count of posts.")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalCount() {
        return ResponseEntity.ok(Map.of("totalPosts", postService.getTotalPostCount()));
    }

    @Operation(summary = "Update a post", description = "Updates the specified post with the provided details.")
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest req, Authentication auth) {
        Post updated = postService.updatePost(id, req, auth.getName());
        return ResponseEntity.ok(postService.toResponse(updated));
    }

    @Operation(summary = "Delete a post", description = "Deletes the specified post if it belongs to the authenticated user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        postService.deletePost(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
