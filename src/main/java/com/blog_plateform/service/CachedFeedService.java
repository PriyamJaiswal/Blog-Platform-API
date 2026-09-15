package com.blog_plateform.service;

import com.blog_plateform.dto.responseDto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CachedFeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostService postService;

    private static final String KEY_PREFIX = "feed::";

    public List<PostResponse> getFeed(Long after, int limit) {

        String cacheKey =
                KEY_PREFIX +
                        (after == null ? "start" : after) +
                        ":" +
                        limit;

        // 1. Check Redis
        List<PostResponse> cached =
                (List<PostResponse>) redisTemplate
                        .opsForValue()
                        .get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // 2. Cache MISS → PostgreSQL
        List<PostResponse> fresh = postService.getPostsCursor(after, limit)
                .stream().map(postService::toResponse)
                .collect(Collectors.toCollection(ArrayList::new));

        // 3. Store in Redis for 30 seconds
        redisTemplate.opsForValue()
                .set(
                        cacheKey,
                        fresh,
                        Duration.ofSeconds(30)
                );

        return fresh;
    }
}
