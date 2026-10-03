package com.blog_plateform.controller;

import com.blog_plateform.dto.requestDto.CommentRequest;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import com.blog_plateform.entity.Comment;
import com.blog_plateform.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Add a comment to a post", description = "Adds a new comment to the specified post.")
    @PostMapping
    public ResponseEntity<Comment> add(@PathVariable Long postId, @Valid @RequestBody CommentRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(postId, req, auth.getName()));
    }

    @Operation(summary = "Get all comments for a post", description = "Retrieves all comments for the specified post.")
    @GetMapping
    public ResponseEntity<List<Comment>> getAll(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    @Operation(summary = "Delete a comment", description = "Deletes the specified comment if it belongs to the authenticated user.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId, @PathVariable Long commentId, Authentication auth) {
        commentService.deleteComment(commentId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
