package com.blog_plateform.service;

import com.blog_plateform.dto.requestDto.CommentRequest;
import com.blog_plateform.entity.Comment;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.CommentRepo;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepository;
    private final PostRepo postRepository;
    private final UserRepo userRepository;

    @CacheEvict(value = "postDetail", key = "#postId")
    public Comment addComment(Long postId, CommentRequest req, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setContent(req.getContent());
        comment.setPost(post);
        comment.setAuthor(author);
        return commentRepository.save(comment);
    }

    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    @CacheEvict(value = "postDetail", key = "#postId")
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Not your comment");
        }
        commentRepository.delete(comment);
    }
}
