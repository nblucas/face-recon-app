# Backend endpoints

REST API, versioned under `/api/v1`. See [`../common/spec.md`](../common/spec.md) for the functional requirements and [`dependencies.md`](dependencies.md) for the underlying stack.

## Create user (singular registration)

`POST /api/v1/users`

Registers a single user. Multiple/batch registration (with threading) is a separate endpoint — see "Create users (batch registration)" below.

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

## Identify user

`POST /api/v1/users/identify`

Given only a picture, finds the registered user whose face best matches it, if any. See [`../common/spec.md`](../common/spec.md) for the facial identification requirement and the detect/extract/compare/threshold steps.

### Request

`multipart/form-data` with a single part:

- `picture`: image file, required.

### Behavior

1. Validate `picture` is a genuine image, same check as registration.
2. Detect a face in the picture and extract its embedding — same pipeline and validations as registration (exactly one face required).
3. Compare the extracted embedding against every registered user's stored embedding (fetched paginated, compared in parallel), by cosine similarity.
4. If the highest similarity found is at or above the configured match threshold, that user is the identification result; otherwise, there is no match.

### Responses

- `200 OK`: always returned when the request itself is valid, whether or not a match was found. Body: `{"identified": boolean, "user": {...} | null}` — `user` is the matched user (same shape as the other user responses) when `identified` is `true`, `null` otherwise. A non-match is not treated as an error.
- `400 Bad Request`: missing or non-image picture, or no face (or more than one face) detected — same validation as registration.

## Verify user

`POST /api/v1/users/verify`

Given a CPF and a picture, checks whether the picture's face matches the registered user's stored embedding. See [`../common/spec.md`](../common/spec.md) for the facial verification requirement and the detect/extract/compare/threshold steps.

### Request

`multipart/form-data` with two parts:

- `request` (JSON):
  ```json
  {
    "cpf": "string, required"
  }
  ```
- `picture`: image file, required.

### Behavior

1. Validate `cpf` format (required, 11 digits, valid check digits).
2. Look up the registered user with that CPF. If none exists, the request is rejected — unlike identification, a non-match on an unknown CPF is treated as an error, since the caller is asserting a specific identity.
3. Validate `picture` is a genuine image, same check as registration.
4. Detect a face in the picture and extract its embedding — same pipeline and validations as registration (exactly one face required).
5. Compare the extracted embedding against the looked-up user's stored embedding, by cosine similarity, against the same match threshold used by identification.

### Responses

- `200 OK`: request valid, comparison performed. Body: `{"matched": boolean}`.
- `400 Bad Request`: missing/invalid CPF format, missing or non-image picture, or no face (or more than one face) detected.
- `404 Not Found`: no registered user has the given CPF.

## Create users (batch registration)

`POST /api/v1/users/batch`

Registers up to 8 users in a single request, with all-or-nothing semantics: either every user in the batch is created, or none is. See [`../common/spec.md`](../common/spec.md) for the multiple-registration requirement.

### Request

`multipart/form-data` with two parts:

- `request` (JSON):
  ```json
  {
    "users": [
      {"clientId": "string, required, unique within the request", "name": "string, required", "cpf": "string, required, unique"},
      ...
    ]
  }
  ```
- `pictures`: one image file per entry in `users`, in any order. **Each picture's filename must be its entry's `clientId`, with the original extension kept** (e.g. `0.jpg` for the entry with `"clientId": "0"`) — this is how the server matches each picture to its corresponding entry in `users`, since HTTP multipart doesn't otherwise preserve a reliable association between separately-named parts. `clientId` is an opaque value chosen by the caller purely for this correlation (e.g. the entry's position in the list) — it is never stored and never appears in the response; in particular, it is **not** meant to carry the CPF or any other personal data.

### Behavior

1. Validate the batch has between 1 and 8 users.
2. Validate there are no duplicate CPFs, and no duplicate `clientId`s, among `users`.
3. Match every picture to its `users` entry by `clientId` — every entry must have exactly one matching picture and vice versa.
4. Validate each entry exactly like singular registration (CPF format/uniqueness, name, picture format).
5. Detect a face and extract its embedding for every picture, in parallel.
6. Only once every entry above passed every check: store all pictures, then insert every user in a single database statement.

### Responses

- `201 Created`: every user in the batch was created. Body: `{"users": [...]}` (same shape as the singular user response, one per entry), in the same order as the request's `users`.
- `400 Bad Request`: batch size out of range, duplicate CPF (within the batch or already registered), duplicate `clientId`, a picture whose filename isn't named after a `clientId`, a picture/user without a match, missing/blank fields, invalid CPF format, missing or non-image picture, or no face (or more than one face) detected in any picture. As with singular registration, no user in the batch is created if any of this fails.
