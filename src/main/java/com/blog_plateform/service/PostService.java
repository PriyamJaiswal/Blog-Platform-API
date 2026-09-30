package com.blog_plateform.service;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.CommentRepo;
import com.blog_plateform.repository.LikeRepo;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final LikeRepo likeRepo;
    private final CommentRepo commentRepo;

    public Post createPost(PostRequest request, String username) {
        User author = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setAuthor(author);
        return postRepo.save(post);
    }

    public Post getPostById(Long id) {
        return postRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
    }

    public Post updatePost(Long id, PostRequest request, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only edit your own posts");
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        return postRepo.save(post);
    }

    @Transactional
    public void deletePost(Long id, String username) {
        Post post = getPostById(id);
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("You can only delete your own posts");
        }
        likeRepo.deleteAll(post.getLikes());
        commentRepo.deleteAll(post.getComments());
        postRepo.delete(post);
    }
}
