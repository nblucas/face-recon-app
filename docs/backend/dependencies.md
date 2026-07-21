# Backend dependencies

Maven dependencies decided so far for the Spring Boot backend. See [`../common/spec.md`](../common/spec.md) for the technology decisions these implement.

## Web

- `spring-boot-starter-web` — REST endpoints, embedded server, JSON (de)serialization.
- `spring-boot-starter-validation` — Bean Validation (Jakarta Validation / Hibernate Validator) for declarative payload validation (e.g. required fields on registration).

## Persistence

- `spring-boot-starter-jooq` — Spring Boot's jOOQ integration (`DSLContext`, transaction management).
- `org.postgresql:postgresql` — JDBC driver.
- `flyway-core` + `flyway-database-postgresql` — schema migrations (Postgres support is a separate module since Flyway 10).
- `org.jooq:jooq-codegen-maven` — build-time plugin, generates jOOQ classes from the DB schema (not a runtime dependency).

## Facial recognition

- `ai.djl:api` — DJL core.
- `ai.djl.onnxruntime:onnxruntime-engine` — DJL's ONNX Runtime engine, used for both face detection (SCRFD, `det_10g.onnx`) and face embedding (ArcFace, `w600k_r50.onnx`), both from the InsightFace `buffalo_l` bundle.
- `org.bytedeco:opencv` (classifiers `linux-x86_64` and `linux-arm64`, not `javacv-platform`) — used only for face alignment (`estimateAffinePartial2D` + `warpAffine`) before the embedding step. Not used for detection: `ai.djl:api`'s default `BufferedImageFactory` (backed by `javax.imageio.ImageIO`) already handles image loading without any native dependency, and detection runs through the ONNX Runtime engine like embedding does.
- The `buffalo_l` model bundle (detection + embedding, ~350MB) is not a Maven artifact — it's downloaded and cached automatically by DJL at runtime (`Criteria.optModelUrls(...)`, cached under `~/.djl.ai/`), not committed to the repo and not requiring any manual setup step.

## Testing

- `spring-boot-starter-test` — includes JUnit 5 (Jupiter), Mockito, and AssertJ out of the box.
- `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter` — integration tests against a real Postgres instance.
