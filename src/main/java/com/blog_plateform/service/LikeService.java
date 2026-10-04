package com.blog_plateform.service;

import com.blog_plateform.entity.Like;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import com.blog_plateform.repository.LikeRepo;
import com.blog_plateform.repository.PostRepo;
import com.blog_plateform.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepo likeRepository;
    private final PostRepo postRepository;
    private final UserRepo userRepository;

    @CacheEvict(value = "postDetail", key = "#postId")
    @Transactional
    public boolean toggleLike(Long postId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Optional<Like> existing = likeRepository.findByPostIdAndUserId(postId, user.getId());

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false;
        } else {
            Post post = postRepository.findById(postId).orElseThrow();
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);
            return true;
        }
    }

    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }
}
