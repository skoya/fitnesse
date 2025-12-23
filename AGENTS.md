# AGENTS.md

## Scope and intent
- These instructions apply to Codex and GitHub Copilot when making changes in this repo.
- Keep User Guides current as features evolve, and surface UX, security, and configuration-ease guidance as part of every change.

## Documentation responsibilities
- When implementing or modifying features, update the relevant User Guide pages in `FitNesseRoot/FitNesse/UserGuide` so the docs stay accurate.
- Make use of plantuml and mermaid diagrams in the documentation to describe things more clearly. 
- If a feature changes user-visible behavior, add a short UX-focused note: what changed, why it matters, and how to use it.
- For security-related changes, document defaults, risks, and safe configuration guidance in the User Guide.
- For configuration changes, provide simple examples and call out the easiest/safest path to adoption.
- If the change introduces new options, update any related "Components and Licenses" or admin docs that a user would reference to enable it.

## Required checks before finishing
- Confirm new settings, routes, or capabilities are reflected in the User Guide.
- If documentation is missing or ambiguous, propose a concrete update (page + section) before marking work complete.
- If unsure where to document, prefer `FitNesseRoot/FitNesse/UserGuide/AdministeringFitNesse` and link from other relevant pages.

## Review prompt
When responding to changes, include a brief suggestion (1-3 bullets) for improving user experience, security posture, or configuration clarity in the User Guide.
