package com.blog_plateform.service;

import com.blog_plateform.dto.requestDto.PostRequest;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;

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
}
