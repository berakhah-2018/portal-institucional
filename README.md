# Portal Institucional

Portal web con frontend React y API Java 17 / Spring Boot, organizado como monolito modular con arquitectura hexagonal.

## Requisitos

- Java 17+
- Maven 3.9+
- Node.js 20+ y npm
- Docker Desktop (PostgreSQL local)
- Credenciales OAuth 2.0 de Google para habilitar inicio de sesión y Drive

## Estructura

- `backend/`: API REST, seguridad y adaptadores de infraestructura
- `frontend/`: interfaz React + TypeScript + Vite
- `compose.yaml`: PostgreSQL local

## Desarrollo

1. Copia `.env.example` a `.env` y configura las credenciales de Google. No subas `.env` al repositorio.
2. Inicia PostgreSQL: `docker compose up -d postgres`.
3. En una terminal ejecuta `mvn -pl backend spring-boot:run -Dspring-boot.run.profiles=google`.
4. En otra terminal ejecuta `cd frontend`, `npm install` y `npm run dev`.
5. Abre `http://localhost:5173`. El acceso real se inicia en `/oauth2/authorization/google`.

## Publicar el frontend en GitHub Pages

El workflow `.github/workflows/pages.yml` publica automáticamente `frontend/` al hacer push a `main`. Para un repo de proyecto usa `VITE_BASE_PATH=/portal-institucional/`. En Settings → Pages selecciona GitHub Actions como source. Si despliegas el backend aparte, define la variable de Actions `VITE_API_BASE_URL` con su origen HTTPS. El backend debe permitir ese origen en CORS y configurar `APP_FRONTEND_URL` con la URL Pages.

GitHub Pages sirve archivos estáticos: no ejecuta Java/Spring, PostgreSQL, sesiones OAuth ni el adaptador de Drive. Sin un backend público HTTPS la web solo muestra la vista previa demo; no pongas secretos OAuth en variables `VITE_*`, porque se incorporan al JavaScript público. Publicar desde un repo privado requiere un plan de GitHub que incluya Pages privado; cambiar a público hará visible todo el código.

La interfaz incluye una vista previa con datos ficticios para ADMIN, EDITOR y LECTOR. Esa selección solo es visual y no otorga permisos. En producción, los roles deben asignarse y comprobarse exclusivamente en el backend. Los endpoints de negocio requieren sesión autenticada.

## Google OAuth y roles

En Google Cloud Console configura una aplicación OAuth de tipo Web y agrega `http://localhost:8080/login/oauth2/code/google` como URI de redirección autorizada. Configura `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` como variables de entorno. La aplicación solicita los scopes `openid`, `profile`, `email` y `https://www.googleapis.com/auth/drive.file`, además de consentimiento offline para refrescar el acceso a Drive. Los tokens autorizados se mantienen del lado servidor en la sesión y no se envían al navegador. El perfil `google` activa este flujo; sin él, el backend puede iniciar con login desactivado.

Define `APP_AUTH_ROLE_MAPPINGS` en el servidor como una lista de pares `correo=ROL`, separados por comas, por ejemplo `admin@ejemplo.com=ADMIN,editor@ejemplo.com=EDITOR`. Los usuarios autenticados no incluidos tendrán rol LECTOR. Nunca se aceptan roles desde el navegador.

El scope `drive.file` limita el acceso a los archivos creados o seleccionados para esta aplicación; no equivale a acceso general a todo Drive. Las rutas `GET /api/images`, `POST /api/images` y `GET /api/images/{id}/content` usan el token del usuario autenticado. La visibilidad se aplica en el backend y las imágenes se sirven a través de la API, nunca con URLs de Drive en el cliente.

## Informes

El endpoint `/api/reports/summary.pdf` produce un PDF con los indicadores permitidos para la sesión. Por ahora los indicadores son datos demo; el MVP debe conectarlos a repositorios reales antes de uso operativo.

## Estado de integraciones

Sin credenciales OAuth y sin activar el perfil `google`, Google Login y Drive no estarán disponibles. La interfaz de demostración no simula una autenticación real. El esquema inicial de PostgreSQL está en Flyway; los indicadores aún usan el adaptador demo. No hay secretos reales en este repositorio.
