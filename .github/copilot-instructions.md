# Portal Institucional

- Monorepo: Spring Boot 3 / Java 17 backend in `backend/`, React + TypeScript + Vite frontend in `frontend/`.
- Keep backend dependencies flowing inward: adapters call application ports; domain code must not depend on Spring or infrastructure.
- Enforce authentication, role checks, image visibility, and report access on the server. Never trust a role or authorization decision from the browser.
- Google OAuth roles come from verified account emails configured through `APP_AUTH_ROLE_MAPPINGS`; unmatched users receive `LECTOR`.
- Drive access uses the signed-in user's OAuth token and the narrow `drive.file` scope. Do not expose Drive URLs or tokens to the browser.
- Keep secrets in the ignored root `.env`; update `.env.example` when adding configuration. Never commit credentials.
- Dashboard metrics currently use an explicitly marked demo adapter. Replace that adapter with persistence-backed data before operational use.
- Frontend preview roles and sample metrics are demo-only. Keep that distinction visible and avoid implying they grant real access.
- Run the backend with Maven from the repository root and the frontend from `frontend/`; see `README.md` for commands and Google setup.
