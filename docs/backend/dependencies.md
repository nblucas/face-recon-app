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
- `org.bytedeco:javacv-platform` — OpenCV bindings (face detection), bundled with native libs for the build platform.
- DJL inference engine (e.g. `ai.djl.onnxruntime:onnxruntime-engine` or `ai.djl.pytorch:pytorch-engine`) — **not decided yet**, depends on the pretrained face embedding model chosen.

## Testing

- `spring-boot-starter-test` — includes JUnit 5 (Jupiter), Mockito, and AssertJ out of the box.
- `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter` — integration tests against a real Postgres instance.
