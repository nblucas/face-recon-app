# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language policy

All code, comments, identifiers, commit messages, and documentation (including files under `docs/`) must be written in English, regardless of the language used in the prompt/conversation.

## Code quality

Follow clean code principles: keep methods short and focused on a single task, separate responsibilities across classes/modules rather than piling logic into one place, and write tests whenever functionality is created or updated.

## Commands

All commands run from the `frontend/` directory.

```bash
npm install       # install dependencies
ng serve          # start dev server at http://localhost:4200 (development config, live reload)
ng build           # production build, output to dist/
ng build --configuration development  # dev build (unminified, source maps)
ng test            # run unit tests via Vitest
ng lint             # lint via @angular-eslint (requires Node >= 20.19)
```

To run a single test file or test name with Vitest, use standard Vitest CLI filters, e.g.:

```bash
ng test -- src/app/app.spec.ts
ng test -- -t "should create the app"
```

See [`README.md`](README.md) for an overview of the stack and how to run the project.
