# Facial Recon

Facial registration, identification and verification, with an Angular frontend and a Spring Boot backend.

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

See [`CLAUDE.md`](CLAUDE.md) for local development commands (backend and frontend run separately, against a locally installed Postgres).

## Documentation

See [`docs/`](docs/) for architecture and subsystem-level documentation.
