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
