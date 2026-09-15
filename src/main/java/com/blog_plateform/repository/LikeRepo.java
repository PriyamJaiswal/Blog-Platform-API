package com.blog_plateform.repository;

import com.blog_plateform.entity.Like;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeRepo extends JpaRepository<Like, Long> {

    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);

    @Query("SELECT l.post.id AS postId, COUNT(l) AS likeCount FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
}