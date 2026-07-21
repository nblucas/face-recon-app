# Facial Recon

Facial registration, identification and verification, with an Angular frontend and a Spring Boot backend.

## Stack

- **Frontend**: Angular 21, standalone components (`frontend/`).
- **Backend**: Java 21 / Spring Boot, built with Maven (`backend/`).
- **Database**: PostgreSQL, accessed via jOOQ, schema managed with Flyway migrations.
- **Face recognition**: [DJL](https://djl.ai/) on the ONNX Runtime engine, using the InsightFace `buffalo_l` bundle — SCRFD for face detection, ArcFace for face embedding. The model bundle (~350MB) is downloaded and cached at runtime, not committed to the repo.
- Pictures are stored on the filesystem; only a reference is kept in the database.

## Running with Docker

Requires Docker Engine (or Docker Desktop, which bundles it) and Docker Compose V2 (the `docker compose` subcommand — not the older standalone `docker-compose` binary). No local Java, Maven, Node or Postgres needed: the build steps for both services run entirely inside their own images.

1. Copy the example environment file and adjust it if you want (the defaults work as-is for a local run):
   ```
   cp .env.example .env
   ```
2. Start everything:
   ```
   docker compose up --build
   ```
3. Open `http://localhost:8080`.

The first startup downloads the face recognition models (~350MB) and takes a bit longer — subsequent restarts reuse the cached models via a named volume.

To reset everything (database, stored pictures, cached models):
```
docker compose down -v
```

## Running without Docker

Requires a local PostgreSQL instance, with credentials matching `FR_USER`/`FR_PW`/`POSTGRES_DB` from `.env.example` (or your own `.env`).

**Backend** (from `backend/`):
```bash
./mvnw spring-boot:run
```

**Frontend** (from `frontend/`), see [`CLAUDE.md`](CLAUDE.md) for the full command list:
```bash
npm install
ng serve
```
