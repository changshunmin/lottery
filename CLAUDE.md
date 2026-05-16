# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 2.7.18 application for managing lottery results. Uses JPA for data access and MySQL for persistence. Java target version: 1.8.

## Build and Run

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Or run the JAR directly
java -jar target/myapi-0.0.1-SNAPSHOT.jar
```

## Database Configuration

MySQL connection configured in `src/main/resources/application.properties`:
- Database: `lottery_db` on localhost:3306
- Uses JPA with `ddl-auto=update` (schema auto-created on startup)

## Architecture

Three-layer Spring Boot architecture:

1. **Entity** (`com.example.myapi.model.LotteryResult`): JPA entity with fields:
   - `id`: Long, auto-generated primary key
   - `prize`: String, the prize description
   - `createdAt`: LocalDateTime, immutable after creation
   - `claimedBy`: String, claimant name
   - `claimedAt`: LocalDateTime, when prize was claimed

2. **Repository** (`com.example.myapi.repository.LotteryResultRepository`): Standard JPA repository extending `JpaRepository`.

3. **Controller** (`com.example.myapi.controller.LotteryController`): REST endpoints under `/api/lottery`:
   - `POST /api/lottery/submit`: Save a new lottery result
   - `GET /api/lottery/all`: Get all results
   - `POST /api/lottery/{id}/claim`: Claim a prize by ID (rejects if already claimed)

## Application Entry

`Main.java` is the Spring Boot application entry point. Note that it currently contains demo code (multiplication table print) that should be removed before production use.

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
