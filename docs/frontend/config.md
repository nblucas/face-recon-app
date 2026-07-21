# Frontend configuration

Angular 21 single-page app (project name `facial-recon-client`), located in `frontend/`. Standalone components only — no `NgModule`s anywhere in the app.

## Bootstrap flow

1. `src/main.ts` calls `bootstrapApplication(App, appConfig)`.
2. `src/app/app.config.ts` provides app-wide services: `provideBrowserGlobalErrorListeners()` and `provideRouter(routes)`.
3. `src/app/app.routes.ts` holds the `Routes` array — currently empty. New routes get added here and referenced from `app.config.ts`'s `provideRouter`.
4. `App` (`src/app/app.ts` + `app.html`) is the root shell. It renders `Navbar` (`src/app/navbar/`) inside a sidebar, and a `<router-outlet>` for routed content.

## Component structure

Each component is a self-contained folder under `src/app/<name>/` with three files:

- `<name>.ts` — standalone `@Component`, explicit `imports` array (no shared module to lean on)
- `<name>.html` — template
- `<name>.css` — component-scoped styles

`ng generate component <name>` produces this shape automatically; follow it for anything new.

## Styling

- Global styles: `frontend/src/styles.css`
- Component-local styles: co-located `<name>.css` next to each component

## Conventions

- TypeScript `strict` mode, plus `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`, and Angular's `strictTemplates` / `strictInjectionParameters` / `strictInputAccessModifiers` (`tsconfig.json`). Keep new code compliant.
- Prettier (`.prettierrc`): 100-char print width, single quotes, Angular parser for `*.html`.
- `.editorconfig`: single quotes in `*.ts`, 2-space indentation everywhere.

## Linting and formatting

- Linting is `@angular-eslint` + `typescript-eslint`, configured in `eslint.config.js` (flat config). Rules of note: `@angular-eslint/component-selector` enforces `app-` prefix + kebab-case on component selectors, `@angular-eslint/directive-selector` enforces `app` prefix + camelCase on directives. `*.html` templates get `angular.configs.templateRecommended` + `templateAccessibility`.
- Run via `ng lint` (aliased as `npm run lint`). Requires Node >= 20.19 — the Angular CLI itself refuses to run on older versions.
- Formatting is Prettier, not ESLint — the two aren't wired together with `eslint-config-prettier`, so keep that in mind if a rule conflict ever shows up.
- VS Code: `.vscode/settings.json` sets Prettier as the default formatter with `editor.formatOnSave: true`, and runs `source.fixAll.eslint` on save. `.vscode/extensions.json` recommends `esbenp.prettier-vscode` and `dbaeumer.vscode-eslint` — install both for this to work.

## Current state / known gaps

- `app.routes.ts` is empty — no routed views exist yet, everything renders in the static shell.
- `Navbar` is a minimal placeholder (renders a title only, no nav links/logic yet).
- No backend integration exists yet — this doc will need an API/data-layer section once one is added.
