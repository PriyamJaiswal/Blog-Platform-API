# Blog Platform API

A production-style blog backend built with **Spring Boot**, featuring JWT authentication, relational data modeling, dual pagination strategies, Redis caching (cache-aside pattern), and integration tests run against real infrastructure via Testcontainers.

## Why this project

Most CRUD-only portfolio projects stop at "it works." This one is built around a measurable performance story: identify a real bottleneck (an N+1 query problem), fix it, benchmark it, then layer Redis caching on top and prove the improvement with numbers — not just claims.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL (hosted on [Neon](https://neon.tech)) |
| Caching | Redis (hosted on [Upstash](https://upstash.com)) |
| Auth | JWT (Spring Security) |
| ORM | Spring Data JPA / Hibernate |
| Testing | JUnit 5, Mockito, Testcontainers (Postgres + Redis) |
| API Testing | Postman (Collection Runner for benchmarking) |

## Features

- **JWT-based authentication** — stateless auth with register/login endpoints, `BCrypt` password hashing.
- **Posts, Comments, Likes** — full CRUD with ownership checks (only the author can update/delete their own content).
- **Dual pagination strategies:**
    - Offset-based (`/api/posts?page=0&size=10`) — simple, familiar.
    - Cursor-based (`/api/posts/feed?after={id}&limit=10`) — scalable, avoids the performance cost of large `OFFSET` values.
- **Redis caching (cache-aside pattern):**
    - `postDetail` cache — annotation-based (`@Cacheable`), 10-minute TTL.
    - `postFeed` cache — manually implemented cache-aside for the cursor feed, 30-second TTL, to explicitly demonstrate the check → miss → populate flow.
    - Pattern-based cache invalidation (`@CacheEvict` + Redis `SCAN`-based key deletion) on post update, delete, like/unlike, and comment changes — prevents stale reads.
- **N+1 query elimination** — batch-fetch queries (`JOIN FETCH`, grouped aggregate counts) instead of per-row queries for likes/comments, since this was the actual first-order performance fix, before caching was even introduced.
- **Automated tests** — unit tests (Mockito) for service logic, integration tests (Testcontainers) covering CRUD, auth, and cache hit/miss/invalidation behavior against real Postgres and Redis containers.

## Architecture

```
Client
  │
  ▼
JWT Auth Filter ──► Spring Security
  │
  ▼
Controller Layer  (PostController, CommentController, LikeController, AuthController)
  │
  ▼
Service Layer  (PostService, CachedFeedService, LikeService)
  │             │
  │             ▼
  │        Redis (Upstash) — cache-aside: check → miss → DB → populate (TTL)
  ▼
Repository Layer (Spring Data JPA)
  │
  ▼
PostgreSQL (Neon)
```

## Performance Results

Benchmarked with Postman Collection Runner (30 iterations per endpoint) against a dataset of 100+ posts, each with 10+ comments and likes.

| Stage | `GET /posts` (list) | `GET /posts/feed` (cursor) | `GET /posts/{id}` |
|---|---|---|---|
| Initial (N+1 bug + cross-region DB + Hikari issues) | ~7,400–7,850 ms | ~7,100–7,400 ms | ~1,600 ms |
| Fully optimized, pre-Redis (N+1 fix + region fix + Hikari tuning) | ~434–539 ms | ~319–429 ms | ~519–530 ms |
| After Redis caching (cache hit) | ~189–193 ms | ~130–150 ms | ~128–130 ms |

**Overall average across 90 requests: 5,389 ms → 469 ms (~11.5x improvement) through query and infrastructure optimization alone, then 469 ms → 209 ms (an additional ~2.2x) after layering in Redis caching — a combined ~25.8x improvement over the original baseline.** See [Engineering Challenges & Solutions](#engineering-challenges--solutions) below for the full breakdown of what caused the slowness and how each issue was diagnosed and fixed. Redis caching is layered on top of this already-optimized baseline, so its improvement number reflects genuine cache-hit savings rather than masking an unoptimized query path.

Note: the Redis-layer improvement here is smaller than the "10–20x" figures often quoted for caching, because Upstash's free tier is hosted in `ap-southeast-1` (Singapore) — round-trip network latency to the cache dominates over the (sub-millisecond) in-memory lookup itself. This was verified directly: cache keys and their TTLs were inspected via the Upstash Data Browser to confirm hits weren't silently falling through to the database.

## Engineering Challenges & Solutions

During development, I ran into three distinct performance/reliability issues before implementing Redis caching. I benchmarked each fix independently using Postman (30-iteration Collection Runner) so every improvement is backed by measured numbers, not assumptions.

### Baseline (before any fixes)

| Endpoint | Avg Response Time |
|---|---|
| `GET /api/posts?page=0&size=10` | ~7,400–7,850 ms |
| `GET /api/posts/feed?limit=10` | ~7,100–7,400 ms |
| `GET /api/posts/{id}` | ~1,600 ms |
| **Overall average (90 requests)** | **5,389 ms** |

---

### Problem 1: N+1 Query Problem on List Endpoints

**Symptom:** Paginated list endpoints (`/api/posts`, `/api/posts/feed`) were taking 7+ seconds despite returning only 10 records.

**Root cause:** The response-mapping logic ran two extra queries *per post* to compute like count and comment count, plus a lazy-loaded query for the author on each post. For a 10-post page, this generated roughly:
```
1 (list query) + 10 × 2 (like/comment counts) + 10 (lazy author) = 31 queries per request
```

**Fix:**
1. Used `JOIN FETCH` in the repository query to eagerly load the post's author in a single query, eliminating the lazy-load N+1:
   ```java
   @Query("SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.createdAt DESC")
   Page<Post> findAllWithAuthor(Pageable pageable);
   ```
2. Replaced per-post count queries with a single **batched GROUP BY query** that fetches like/comment counts for all posts on the page at once:
   ```java
   @Query("SELECT l.post.id AS postId, COUNT(l) AS likeCount FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
   List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);
   ```
3. Split the mapping logic into two methods: `toResponse()` for single-post detail (where one query is fine) and `toResponseList()` for batch list endpoints (which uses the aggregated counts).

**Result:**

| Endpoint | Before | After | Improvement |
|---|---|---|---|
| `GET /api/posts?page=0&size=10` | ~7,400–7,850 ms | ~1,190–1,208 ms | ~6.5x faster |
| `GET /api/posts/feed?limit=10` | ~7,100–7,400 ms | ~1,197–2,350 ms | ~3.5x faster |
| `GET /api/posts/{id}` | ~1,600 ms | ~1,516–1,561 ms | No change (expected — this endpoint was never affected by N+1) |

---

### Problem 2: Database Region Mismatch (High Network Latency)

**Symptom:** Even after fixing N+1, `GET /api/posts/{id}` — a single-row query with no joins — still took ~1.5 seconds. That's abnormally slow for 2–3 simple queries.

**Root cause:** The Neon Postgres project was provisioned in **AWS US East 2 (Ohio)**, while the application and its users are based in India. Every database round trip was crossing roughly half the globe, adding ~250–300ms of pure network latency *per query* — independent of how fast the query itself executed.

**Fix:** Created a new Neon project in **AWS Asia Pacific (Singapore) — ap-southeast-1**, the closest available Neon region to India (Neon does not yet support a Mumbai region).

**Data migration (existing data — 100 posts, ~1,000 comments/likes):**
Since a new Neon project starts empty, existing data was migrated using `pg_dump`/`psql` rather than re-seeded, to preserve realistic testing data.

```bash
# 1. Dump from the old (Ohio) project
pg_dump "postgresql://user:password@old-project.us-east-2.aws.neon.tech/neondb?sslmode=require" \
  --no-owner --no-privileges --format=plain \
  --file=blog_backup.sql

# 2. Clear the new project's auto-generated empty schema
#    (Hibernate's ddl-auto=update creates empty tables on first app boot,
#    which conflicts with restoring a schema that already has those tables)
psql "postgresql://user:password@new-project.ap-southeast-1.aws.neon.tech/neondb?sslmode=require" \
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 3. Restore into the new (Singapore) project
psql "postgresql://user:password@new-project.ap-southeast-1.aws.neon.tech/neondb?sslmode=require" \
  --file=blog_backup.sql

# 4. Verify row counts match the original
psql "postgresql://user:password@new-project.ap-southeast-1.aws.neon.tech/neondb?sslmode=require" \
  -c "SELECT COUNT(*) FROM posts;"
```

`--no-owner --no-privileges` was used because Neon assigns different internal role names per project, and copying ownership/privilege statements across projects causes restore errors.

**Result:** Combined with the N+1 fix, `GET /api/posts/{id}` dropped from ~1,516ms to ~519–530ms — roughly a 3x additional improvement purely from removing cross-continent latency.

---

### Problem 3: HikariCP Connection Pool Failures After Migration

**Symptom:** After switching to the new Neon project, the application logs showed:
```
WARN ... HikariPool-1 - Failed to validate connection ... (This connection has been closed.)
Possibly consider using a shorter maxLifetime value.
```

**Root cause:** Neon's serverless Postgres closes idle connections server-side (as part of its auto-suspend/pooling behavior) after a few minutes. Spring Boot's default HikariCP pool settings weren't aware of this and kept trying to reuse connections that Neon had already dropped, causing failed queries and warnings.

**Fix:** Tuned Hikari's pool lifecycle settings so the pool proactively retires connections *before* Neon closes them server-side:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      idle-timeout: 30000
      max-lifetime: 270000       # 4.5 min — shorter than Neon's server-side timeout
      connection-timeout: 30000
      keepalive-time: 120000     # pings idle connections every 2 min
      validation-timeout: 5000
```

Also switched to Neon's **pooled connection string** (the `-pooler` endpoint variant, which routes through PgBouncer) for better handling of connection churn in a serverless database environment.

**Result:** Zero connection errors across a 30-iteration/90-request load test after the fix, with no correctness impact — this was a stability fix rather than a raw speed fix.

---

### Combined Impact (before any fix → fully optimized, pre-Redis → with Redis)

| Metric | Before | After (N+1 fix + region fix + Hikari tuning) | After Redis caching |
|---|---|---|---|
| `GET /api/posts?page=0&size=10` | ~7,400–7,850 ms | ~434–539 ms | ~189–193 ms |
| `GET /api/posts/feed?limit=10` | ~7,100–7,400 ms | ~319–429 ms | ~130–150 ms |
| `GET /api/posts/{id}` | ~1,600 ms | ~519–530 ms | ~128–130 ms |
| **Overall average (90 requests)** | **5,389 ms** | **469 ms** | **209 ms** |

**~11.5x improvement from query optimization, infrastructure placement, and connection pool tuning alone — then a further ~2.2x from Redis caching, for a combined ~25.8x improvement over the original baseline.** The optimization work happened *before* caching was introduced specifically so the Redis number would reflect genuine cache-hit savings rather than mask an already-fixable bottleneck.

### Why this matters (talking points for interviews)

- **Measure before optimizing:** Every fix was validated with the same 30-iteration Postman benchmark, so each number is a controlled before/after comparison rather than a guess.
- **N+1 queries are often invisible until load-tested** — the code looked correct and passed functional tests; only a response-time benchmark exposed the problem.
- **Database region is a real production consideration**, not just a caching/algorithm concern — network physics can dominate over query efficiency.
- **Serverless databases have different connection lifecycle behavior** than traditional always-on Postgres, and connection pool defaults tuned for the latter can silently fail on the former.

## API Endpoints

### Auth
| Method | Endpoint | Auth required |
|---|---|---|
| POST | `/api/auth/register` | No |
| POST | `/api/auth/login` | No |

### Posts
| Method | Endpoint | Auth required |
|---|---|---|
| POST | `/api/posts` | Yes |
| GET | `/api/posts?page=0&size=10` | No |
| GET | `/api/posts/feed?after={id}&limit=10` | No |
| GET | `/api/posts/{id}` | No |
| PUT | `/api/posts/{id}` | Yes (owner only) |
| DELETE | `/api/posts/{id}` | Yes (owner only) |

### Comments
| Method | Endpoint | Auth required |
|---|---|---|
| POST | `/api/posts/{postId}/comments` | Yes |
| GET | `/api/posts/{postId}/comments` | No |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | Yes (owner only) |

### Likes
| Method | Endpoint | Auth required |
|---|---|---|
| POST | `/api/posts/{postId}/likes/toggle` | Yes |

## Testing

**[10] test cases** across unit and integration layers. Integration tests run against **real PostgreSQL and Redis containers** via Testcontainers — not mocks — so cache hit/miss/eviction behavior is verified against actual infrastructure, the same way it would run in production.

| Layer | What's covered | Tooling |
|---|---|---|
| Unit tests | Service-layer logic: post creation, ownership checks on update/delete, not-found handling | JUnit 5, Mockito |
| Integration — CRUD & Auth | Full request flow through the controller: create → fetch, unauthenticated requests rejected (401), non-owner updates rejected (403) | JUnit 5, Spring MockMvc, Testcontainers (PostgreSQL) |
| Integration — Caching | Cache-hit correctness (second call served from Redis, not DB), cache invalidation on update (stale data isn't served post-write) | JUnit 5, Testcontainers (PostgreSQL + Redis) |

```bash
# requires Docker running locally (Testcontainers spins up Postgres + Redis)
mvn test
```

## Running Locally

### Prerequisites
- Java 21
- Maven
- A Neon PostgreSQL database (free tier)
- An Upstash Redis database (free tier)
- Docker (required for running the integration test suite via Testcontainers)

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/PriyamJaiswal/<repo-name>.git
   cd <repo-name>
   ```

2. Set environment variables (or use a `.env` file with a loader):
   ```
   DB_URL=jdbc:postgresql://<neon-host>/<dbname>?sslmode=require
   DB_USERNAME=<neon-username>
   DB_PASSWORD=<neon-password>
   JWT_SECRET=<your-secret-key>
   JWT_EXPIRATION=86400000
   REDIS_HOST=<upstash-host>
   REDIS_PORT=<upstash-port>
   REDIS_PASSWORD=<upstash-password>
   ```

3. Run the app:
   ```bash
   mvn spring-boot:run
   ```

4. Run tests:
   ```bash
   mvn test
   ```

## What I'd improve next

- Move offset/limit pagination fully to cursor-based pagination across all list endpoints, since it scales better under large datasets.
- Add rate limiting on auth endpoints.
- Introduce a message queue (Kafka) for async notification on new comments/likes, decoupling it from the request path.

## Author

**Priyam Jaiswal**
- GitHub: [github.com/PriyamJaiswal](https://github.com/PriyamJaiswal)
- LinkedIn: [linkedin.com/in/priyamjaiswal608](https://linkedin.com/in/priyamjaiswal608)