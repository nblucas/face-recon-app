User registration:
- To register an user, name, CPF and picture are needed. All fields are required.
- The register has two modes: singular and multiple. Singular registration is for one user registration, multiple is for N users being register in batch.
- In multiple registration, threads should be used to speed up registration.

User update:
- Name and picture can be updated.
- Update has the same modes as registration. Threads should also exist for multiple update.

User list:
- A list of users should exist.
- It's from a user row that update can interacted with.
- Other than update, a row should also permit deleting and showing details (modal with users info).
- Every row in list should have name, CPF and picture (just as in details modal - for now, it'll be redundant).
- The listing should be implemented considering offset/limit pagination.

Facial verification:
- Client sends CPF and picture.
- Server sends back if there is or not similarity.

Facial identification:
- Client sends only picture.
- Server send back correspondent user, if there is identification.
- If there is not identification, server informs that also.


Technical points:
- For verification and identification the steps are:
    1. Detect the face on image;
    2. Extract numerical representation of face;
    3. Calculate similarity between faces;
    4. Make decision based on defined threshold: above or equal to threshold, the faces are equal, and below, the faces are different.
- Postgres gonna be the SGBD.
- Java 17 with SpringBoot on backend, built with Maven.
- jOOQ for the persistence/query layer.
- Flyway for database schema migrations.
- REST APIs.
- Face detection and embedding extraction via DJL (Deep Java Library) + JavaCV (OpenCV bindings).
- DJL runs on the ONNX Runtime engine, using the ArcFace model (InsightFace `buffalo_l`/`w600k_r50`, ONNX) for face embedding extraction.
- Pictures are stored on the filesystem, with only a reference stored in the database. Not transactional with the DB write: write the file first, then commit the DB row, to avoid dangling references.

