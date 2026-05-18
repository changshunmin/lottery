# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Lucky Wheel lottery system with separate user lottery and admin management interfaces. Backend is Spring Boot 2.7.18 (Java 1.8) with MySQL. Frontend is Vue 3 + Vite with Element Plus, built into Spring Boot static resources.

## Build and Run

```bash
# Backend
cd /data/code/lottery
mvn clean package
mvn spring-boot:run       # Runs on port 8080, includes hot reload via DevTools

# Frontend development
cd frontend
npm install               # First time
npm run dev              # Dev server on port 5173, proxies /api to localhost:8080
npm run build            # Builds to ../src/main/resources/static/admin/

# Full deployment
cd frontend && npm run build && cd .. && mvn spring-boot:run
```

## Architecture

### Backend Structure - Package `com.lottery`

**Entities:**
- `LotteryResult`: Stores lottery draws (id, prize, createdAt, claimedBy, claimedAt)
- `PrizeConfig`: Prize rules (id, name, icon, probability, sortOrder, createdAt)

**Controllers:**
- `LotteryController` (`/api/lottery`):
  - `POST /submit` - Save lottery result
  - `GET /all` - Get all results
  - `POST /{id}/claim` - Claim prize by ID
- `PrizeController` (`/api/prizes`):
  - `GET /` - Get all prizes (sorted by sortOrder)
  - `POST /batch` - Batch save prizes (replaces all)
  - Initializes default prizes if DB empty on startup

**Static Resources:**
- User lottery page: `src/main/resources/static/` (root path `/`)
- Admin panel: `src/main/resources/static/admin/` (path `/admin/`)

### Frontend Structure - `frontend/`

Vue 3 + Vite + Element Plus admin interface with two tabs:
- **Results Tab**: Lottery record management with search/filter (prize, user, date range), pagination, CSV export, and prize claiming
- **Prizes Tab**: Prize configuration with CRUD operations, probability total validation

**Key Configuration:**
- `vite.config.js`: base path `/admin/`, dev proxy `/api → localhost:8080`
- Build output: `../src/main/resources/static/admin/` (served by Spring Boot)

## Database Configuration

MySQL in `src/main/resources/application.properties`:
- Database: `lottery_db` on localhost:3306
- Schema: Auto-created via JPA `ddl-auto=update`
- Tables: `lottery_result`, `prize_config`

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md