# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language policy

All code, comments, identifiers, commit messages, and documentation (including files under `docs/`) must be written in English, regardless of the language used in the prompt/conversation.

## Repository state

This repository currently contains only the `frontend/` directory: an Angular 21 single-page application scaffold (project name `facial-recon-client`). There is no backend yet — despite the repo name, no facial recognition logic exists in this codebase yet. Treat the app as an early-stage scaffold rather than an established architecture.

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

There is no e2e test setup.

## Subsystem docs

Detailed docs live under `docs/`, one directory per subsystem. Read the relevant one before making non-trivial changes in that area:

- [`docs/frontend/config.md`](docs/frontend/config.md) — Angular app structure, bootstrap flow, component conventions, styling, linting/formatting, known gaps.

(No backend/ML subsystem exists yet — add a doc directory here when one is started.)
