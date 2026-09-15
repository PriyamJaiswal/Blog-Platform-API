package com.blog_plateform.repository;

import com.blog_plateform.entity.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepo extends JpaRepository<Post, Long> {

    List<Post> findByIdLessThanOrderByIdDesc(Long id, Pageable pageable);
    List<Post> findTop10ByOrderByIdDesc();

    // NEW: fetch author eagerly to avoid N+1 on author
    @Query("SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.createdAt DESC")
    Page<Post> findAllWithAuthor(Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id < :id ORDER BY p.id DESC")
    List<Post> findByIdLessThanWithAuthor(@Param("id") Long id, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.id DESC")
    List<Post> findTopWithAuthor(Pageable pageable);
}

