# Blog Platform API

A production-grade REST API for a blog platform built with Spring Boot 4.1.

## Tech Stack
- Java 21, Spring Boot 4.1
- PostgreSQL (Neon serverless)
- Redis (Upstash) with cache-aside pattern
- Spring Security + JWT
- Docker
- Maven

## Architecture

The API follows a layered architecture:
- **Controller** layer handles HTTP requests and authentication
- **Service** layer contains business logic with caching
- **Repository** layer interfaces with PostgreSQL via JPA
- **Security** layer provides JWT-based authentication

## Features
- User registration and authentication with JWT
- CRUD operations for blog posts with ownership enforcement
- Comments with owner-only deletion
- Like/unlike toggle for posts
- Dual pagination: offset-based and cursor-based (keyset)
- Redis cache-aside pattern with per-cache TTL tuning
- N+1 query optimization with JOIN FETCH

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
