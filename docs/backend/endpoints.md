# Backend endpoints

REST API, versioned under `/api/v1`. See [`../common/spec.md`](../common/spec.md) for the functional requirements and [`dependencies.md`](dependencies.md) for the underlying stack.

## Create user (singular registration)

`POST /api/v1/users`

Registers a single user. Multiple/batch registration (with threading) is a separate endpoint, to be documented once designed.

### Request

`multipart/form-data` with two parts:

- `data` (JSON):
  ```json
  {
    "name": "string, required",
    "cpf": "string, required, unique"
  }
  ```
- `picture`: image file, required.

### Behavior

1. Validate `data` (`name` and `cpf` required).
2. Validate `picture` is a genuine image, checked by content rather than trusting the declared `Content-Type`.
3. Detect a face in the picture. If no face is detected, the request is rejected.
4. Extract the face embedding synchronously — no deferred/background processing, so the embedding is ready before the request completes (see [`../common/spec.md`](../common/spec.md) for the model used).
5. Check CPF uniqueness.
6. Write the picture to the filesystem under a server-generated filename (never the client-supplied one), then commit the database row (see the filesystem storage note in [`../common/spec.md`](../common/spec.md)).
7. Insert the user row: a sequence-generated numeric `id` as primary key (CPF is `UNIQUE`, not the PK), name, CPF, picture reference, embedding.

### Responses

- `201 Created`: user created. Body contains the created user `id`.
- `400 Bad Request`: used uniformly for every rejection reason: missing/blank fields, invalid CPF format, missing or non-image picture, no face detected, or duplicate CPF. These cases deliberately share one status code: CPF is sensitive, and a status code that distinctly signals "this CPF is already registered" would let CPFs be enumerated without even reading the response body. The specific reason is only available in the response body, for clients that are meant to read it.
- `413 Payload Too Large`: picture exceeds the configured size limit.
- `415 Unsupported Media Type`: `picture` part is not an image content type.
