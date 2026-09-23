package com.blog_plateform.repository;

import com.blog_plateform.entity.Like;
import com.blog_plateform.entity.Post;
import com.blog_plateform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepo extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);
}
