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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final UserRepo userRepo;

    public Comment addComment(Long postId, CommentRequest request, String username) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Comment comment = new Comment();
        comment.setBody(request.getBody());
        comment.setPost(post);
        comment.setAuthor(user);
        return commentRepo.save(comment);
    }
}
