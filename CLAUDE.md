# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## 5. No Unauthorized Commits

**Never commit or push unless the user explicitly instructs you to.**
- Stage, commit, and push only when told.
- If you create files or make changes, leave them unstaged/uncommitted until asked.
- The exception: the user says "fix this bug" and you need to demonstrate — still ask before committing.

---



## Project: superGaoS Blog

Spring Cloud Alibaba microservices blog system.

### Build & Run

**Backend (Java 17 + Maven):**
```bash
mvn clean compile -DskipTests          # Compile all modules
mvn clean package -DskipTests          # Package all modules
mvn clean package -pl supergaos-blog -am -DskipTests  # Single module
mvn spring-boot:run -pl supergaos-user # Run a service locally
```

**Frontend (Node.js + Vue 3 + Vite):**
```bash
cd frontend && npm install  # Install deps
npm run dev                 # Dev server (hot reload)
npm run build               # Production build
```

**Docker:**
```bash
docker-compose up -d --build            # Full stack
docker-compose up -d --build frontend   # Single service
docker-compose logs -f blog-service     # Tail logs
docker-compose down -v                  # Stop + reset DB
```

### Project Structure

```
supergaos-common/    — Shared library (Result<T>, BusinessException)
supergaos-gateway/   — Spring Cloud Gateway, JWT auth filter
supergaos-user/      — Login/register, JWT generation
supergaos-blog/      — Article CRUD, categories, tags, Feign → comment count
supergaos-comment/   — Comments CRUD
supergaos-file/      — File upload/download via MinIO
frontend/            — Vue 3 + Vite + Vue Router + Axios + marked
sql/                 — init.sql: schema + seed data
```

### API Gateway Routes
```
/api/user/**    → supergaos-user (9094)
/api/blog/**    → supergaos-blog (9091)
/api/comment/** → supergaos-comment (9092)
/api/file/**    → supergaos-file (9093)
```

### Key Conventions
- Java 17, Spring Boot 3.2.5, Lombok, MyBatis XML mappers
- API responses wrapped in `Result<T>` — `{ code:200, message:"success", data: T }`
- MySQL with snake_case columns (`create_time`), one DB per service
- Nacos namespace: `supergaos`, service names: `supergaos-{name}`
- Frontend: Composition API (`<script setup>`), Axios with response interceptor
- Branches: `main` (stable), `dev` (active development)

### Common Pitfalls
- `.dockerignore` must exclude `frontend/node_modules/` (Windows permission issues)
- Frontend field names: `createTime` not `createdAt`, `articles` not `records`, status as integer
- Video seeking requires HTTP 206 Partial Content (Range header support)
