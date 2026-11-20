# MediStock

MediStock es una aplicación web para gestionar medicamentos y solicitudes en una red de farmacias asociadas a EPS. Incluye portales para usuarios, administradores de farmacia y gestores, además de herramientas de apoyo para reabastecimiento y optimización operativa.

## Funcionalidades

- Registro e inicio de sesión local, con autenticación opcional mediante Google y GitHub.
- Consulta de farmacias y medicamentos, solicitud de medicamentos y gestión de citas.
- Administración de usuarios, EPS, farmacias, medicamentos, solicitudes y reabastecimientos según el rol.
- Paneles de gestión y un chatbot conectado a la API de Groq.
- Optimización de asignación de administradores y atención de solicitudes mediante un servicio FastAPI con PuLP/CBC. La aplicación Java dispone de una alternativa cuando el servicio no responde.

## Tecnologías

- Java 26 y Spring Boot 4.1.1, con Spring MVC, Thymeleaf, Spring Security y Spring Data JPA.
- MySQL como base de datos principal. El proyecto también incluye el controlador JDBC de PostgreSQL.
- Python, FastAPI, Uvicorn y PuLP para el optimizador.

## Requisitos

- JDK 26.
- MySQL accesible localmente o mediante una URL configurada.
- Python 3.10 o superior para ejecutar el optimizador.
- Maven (o el wrapper incluido `mvnw` / `mvnw.cmd`).

## Configuración

1. Crea una base de datos MySQL, por ejemplo `medistock_db`.
2. Configura las variables de entorno. La aplicación usa estos valores predeterminados para la base de datos: `jdbc:mysql://localhost:3306/medistock_db`, usuario `root` y contraseña tomada de `DB_PASSWORD` (si falta, el valor de respaldo es `configure-me`).
3. Para habilitar el acceso social, configura `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID` y `GITHUB_CLIENT_SECRET` con credenciales OAuth propias. Estos proveedores son opcionales para el inicio de sesión local.
4. Para usar el chatbot, configura `GROQ_API_KEY`. Opcionalmente, puedes definir `GROQ_API_MODEL` y `GROQ_API_URL`.
5. El servicio de optimización escucha por defecto en `http://127.0.0.1:8000`. Puedes cambiar la dirección con `OPTIMIZER_API_URL`.

No guardes credenciales reales en el repositorio. Usa variables de entorno o un archivo local `.env` (ignorado por Git).

## Ejecución

Inicia el optimizador en una terminal:

```bash
cd optimizer-api
python -m venv .venv
# Windows: .venv\Scripts\activate
# Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

En otra terminal, desde la raíz del proyecto, inicia Spring Boot:

```bash
# Windows
./mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

La aplicación queda disponible en [http://localhost:8082](http://localhost:8082). Hibernate actualiza el esquema con `spring.jpa.hibernate.ddl-auto=update`.

## Cuentas temporales de demostración

Existe un seeder opcional bajo el perfil `seed-accounts`. Solo crea las cuentas que aún no existan y requiere `SEED_MANAGER_PASSWORD`, `SEED_ADMIN_PASSWORD` y `SEED_USER_PASSWORD`. Para crear también el administrador, debe existir al menos una EPS. No se deben usar contraseñas de producción para estas cuentas.

## Estructura

```text
src/main/java/       Aplicación Spring Boot, controladores, servicios y entidades
src/main/resources/  Configuración y vistas Thymeleaf
optimizer-api/       API FastAPI para optimización operativa
```
