package com.blog_plateform.controller;

import com.blog_plateform.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "Toggle like for a post", description = "Toggles the like status for the specified post.")
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long postId, Authentication auth) {
        boolean liked = likeService.toggleLike(postId, auth.getName());
        long count = likeService.getLikeCount(postId);
        return ResponseEntity.ok(Map.of("liked", liked, "likeCount", count));
    }
}
