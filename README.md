# Employee Manager: Microservices Project

Employee management system built as two Spring Boot microservices with JWT authentication, MySQL and Docker / Sistema di gestione impiegati realizzato con due microservizi Spring Boot, autenticazione JWT, MySQL e Docker.

**Languages / Lingue:** [English](#english) | [Italiano](#italiano)

---

<a id="english"></a>

# English

## Table of contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Technology stack](#technology-stack)
5. [Project structure](#project-structure)
6. [Data model](#data-model)
7. [Salary and bonus rules](#salary-and-bonus-rules)
8. [Tax calculation](#tax-calculation)
9. [REST API](#rest-api)
10. [Security](#security)
11. [Web interface](#web-interface)
12. [Running the project](#running-the-project)
13. [Configuration](#configuration)
14. [Continuous integration](#continuous-integration)
15. [Known limitations](#known-limitations)

## Overview

Employee Manager is a back-office application made of two independent Spring Boot services that talk to each other over HTTP:

- **Employee Service (MS1, port 8080)** manages employees, users and authentication, serves the web interface and persists data in MySQL.
- **Salary Analytics (MS2, port 8081)** receives an employee's data and returns a complete salary sheet: Italian income tax (IRPEF), social security contributions (INPS), deductions, net pay and a salary score.

Employees do not have a stored salary. The salary is always derived at runtime from the employee's category and seniority, and the annual bonus is derived from the same data. The browser only talks to MS1; MS2 is called by MS1.

Author: Riccardo Croce.

## Architecture

```
Browser (login.html, index.html)
        |
        |  HTTP + JWT (Authorization: Bearer ...)
        v
+---------------------------+   POST /salary/calcola   +---------------------------+
|  MS1  Employee Service    | -----------------------> |  MS2  Salary Analytics    |
|  port 8080                |                          |  port 8081                |
|                           | <---- SalaryResponse --- |                           |
|  - employee CRUD          |                          |  - IRPEF (3 brackets)     |
|  - login / users / roles  |                          |  - work deductions        |
|  - salary and bonus       |                          |  - INPS                   |
|  - serves the web UI      |                          |  - net pay, salary score  |
+-------------+-------------+                          +---------------------------+
              |
              |  JPA / Hibernate
              v
        +-----------+
        |   MySQL   |
        +-----------+
```

MS1 reaches MS2 through the URL set in `salary.service.url` (`http://localhost:8081` locally, `http://ms2-salary:8081` inside the Docker network).

## Features

**Employee management**
- List all employees, search by ID (path or query string) and search by surname.
- Create, fully replace (PUT), partially update (PATCH) and delete employees.
- Validation with readable messages: age greater than zero, mandatory hiring date that is not in the future, unique name and surname pair, unique email.
- Salary, seniority step and annual bonus computed at runtime for every employee.

**Salary analysis**
- On-demand salary sheet for any employee: gross annual and monthly pay, IRPEF by bracket, work deductions, INPS, net annual and monthly pay, effective tax rate, salary score and benchmark.

**Authentication and authorisation**
- Registration and login, stateless JWT, passwords hashed with BCrypt.
- Two roles: `USER` (read-only) and `ADMIN` (full access).
- Administrators can list users and promote or demote them.

**Web interface**
- Single-page application in plain HTML, CSS and JavaScript, with a login and registration page.
- Menus adapt to the role of the logged-in user; errors are shown as toast notifications with readable messages.

**Delivery**
- Docker images for both services, four Docker Compose files for different environments and a GitHub Actions workflow that publishes the images.

## Technology stack

| Area | Technology |
|------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Security) |
| Authentication | JSON Web Tokens (jjwt 0.11.5), BCrypt |
| Database | MySQL 8.3 (Docker) or an external MySQL server |
| Build | Maven (Maven Wrapper included in both services) |
| Front end | HTML5, CSS3, vanilla JavaScript |
| Containers | Docker (multi-stage builds), Docker Compose |
| CI | GitHub Actions, GitHub Container Registry |

## Project structure

```
microservices-project/
├── .github/workflows/
│   └── docker-publish.yml           Builds and publishes the Docker images
├── docker-compose.yml               Prebuilt images from the registry + MySQL container
├── docker-compose.autonomo.yml      Local build + MySQL container (recommended)
├── docker-compose.casa.yml          Local build + external MySQL (home environment)
├── docker-compose.scuola.yml        Local build + external MySQL (school environment)
├── init.sql                         Database initialisation (autonomous profile)
├── ComeUtilizzareDocker.txt         Short Docker instructions (Italian)
├── Documentazione_Progetto_Microservizi.md   Project documentation (Italian)
│
├── employee-service/                MS1
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/employee_service/
│       │   ├── controller/          EmployeeController, AuthController, WelcomeController
│       │   ├── service/             EmployeeService, JwtService, UserDetailsServiceImpl
│       │   ├── security/            SecurityConfig, JwtAuthFilter
│       │   ├── entity/              Employee, SalaryRange, User
│       │   ├── repository/          EmployeeRepo, SalaryRangeRepository, UserRepository
│       │   ├── dto/                 EmployeeRequest, EmployeeResponse, SalaryResponse
│       │   ├── client/              SalaryClient (HTTP client towards MS2)
│       │   └── config/              DataLoader (seed data), EmployeeConfig
│       └── resources/
│           ├── application.properties, application-casa.properties,
│           │   application-scuola.properties
│           └── static/              login.html, index.html, logo.jpg
│
└── salary-analytics/                MS2
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/salary_analytics/
        ├── SalaryController.java    POST /salary/calcola
        ├── SalaryService.java       IRPEF, deductions, INPS, score
        ├── SalaryRequest.java
        └── SalaryResponse.java
```

## Data model

**`employee`**

| Column | Type | Notes |
|--------|------|-------|
| `id` | INT, PK | Auto increment |
| `name`, `cognome` | VARCHAR(100) | First name and surname; the pair must be unique (checked by the service) |
| `email` | VARCHAR(150) | Unique |
| `age` | INT | Must be greater than zero |
| `tipo_impiegato` | VARCHAR(20) | `OPERAIO`, `IMPIEGATO`, `QUADRO` or `DIRIGENTE`; default `IMPIEGATO` |
| `data_assunzione` | DATE | Hiring date, mandatory, not in the future |

**`salary_range`** is a lookup table with one row per category (one category has many employees).

| Column | Type | Notes |
|--------|------|-------|
| `tipo_impiegato` | VARCHAR(20), PK | Category |
| `stipendio_medio` | DOUBLE | Base gross annual salary of the category |
| `descrizione` | VARCHAR(100) | Description |
| `percentuale_bonus` | DOUBLE | Bonus rate (0.08 means 8%) |
| `anni_minimi_bonus` | INT | Years of service required to earn the bonus |

**`users`**

| Column | Type | Notes |
|--------|------|-------|
| `id` | INT, PK | Auto increment |
| `username` | VARCHAR(100) | Unique |
| `password` | VARCHAR(255) | BCrypt hash |
| `ruolo` | VARCHAR(20) | `USER` or `ADMIN` |

The schema is created by Hibernate (`ddl-auto=update`) and, in the autonomous Docker profile, also by `init.sql`. On first start the `DataLoader` inserts the sample data when the tables are empty: 4 salary categories, 8 employees and 4 users.

| Demo account | Password | Role |
|--------------|----------|------|
| `croceadmin` | `beeswarm` | ADMIN |
| `user1`, `user2`, `user3` | `pass1`, `pass2`, `pass3` | USER |

These accounts are for demonstration only. New accounts created from the registration page always have the `USER` role.

## Salary and bonus rules

The salary is never stored. For every response MS1 computes it as:

```
salary = category base salary x seniority multiplier      (rounded to the nearest 100 euro)
```

| Category | Base gross salary | Bonus rate | Years needed for the bonus |
|----------|------------------:|-----------:|---------------------------:|
| `OPERAIO` | 24,000 | 5% | 3 |
| `IMPIEGATO` | 32,000 | 8% | 3 |
| `QUADRO` | 52,000 | 12% | 5 |
| `DIRIGENTE` | 90,000 | 20% | 5 |

| Years of service | Multiplier | Description |
|------------------|-----------:|-------------|
| 0 to 2 | 1.00 | No step |
| 3 to 5 | 1.05 | +5% |
| 6 to 10 | 1.12 | +12% |
| 11 to 20 | 1.20 | +20% |
| more than 20 | 1.30 | +30% |

The **annual bonus** is `salary x bonus rate`, rounded to the nearest 100 euro. It is paid only when the years of service reach the minimum of the category, otherwise it is `0`. Years of service are the whole years between the hiring date and today, so the values change over time.

## Tax calculation

Salary Analytics applies a simplified model of the 2024 Italian rules to the gross annual salary received from MS1.

- **IRPEF brackets:** 23% up to 28,000 euro, 35% from 28,001 to 50,000 euro, 43% above 50,000 euro.
- **Employee deduction** (article 13 TUIR): a decreasing deduction that is zero above 50,000 euro. The IRPEF returned in `irpefTotaleAnnuo` is the amount after the deduction.
- **INPS:** 9.19% of the salary, up to a ceiling of 119,650 euro.
- **Net pay:** gross salary minus net IRPEF minus INPS. Monthly figures are divided by **13** monthly payments.
- **Effective tax rate:** (net IRPEF + INPS) divided by the gross salary.
- **Salary score (0 to 100):** a logistic curve centred on the average Italian salary of about 29,000 euro a year; a score of 50 means in line with the average. The benchmark text is "Sotto la media nazionale" (below 40), "In linea con la media nazionale" (40 to 59), "Sopra la media nazionale" (60 to 79) and "Top 20% nazionale" (80 and above).

The model is educational and is not meant for real payroll.

## REST API

All paths are served by MS1 on port 8080. Protected endpoints need the header `Authorization: Bearer <token>`.

### Authentication and users

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | public | Registers a user with role `USER`. Body: `{"username": "...", "password": "..."}`. Returns 201, or 400 if the username exists |
| POST | `/auth/login` | public | Returns `{"token", "ruolo", "username"}` |
| GET | `/auth/utenti` | ADMIN | Lists all users (id, username, role) |
| PATCH | `/auth/promuovi/{id}` | ADMIN | Promotes a user to `ADMIN` |
| PATCH | `/auth/retrocedi/{id}` | ADMIN | Demotes a user to `USER` |

### Employees

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/impiegati` | USER, ADMIN | Lists all employees |
| GET | `/impiegati/id/{id}` | USER, ADMIN | Finds an employee by ID |
| GET | `/impiegati/cerca?id={id}` | USER, ADMIN | Finds an employee by ID (query string) |
| GET | `/impiegati/cognome/{cognome}` | USER, ADMIN | Finds employees by surname (case sensitive) |
| GET | `/impiegati/{id}/salary` | USER, ADMIN | Salary sheet computed by MS2 |
| POST | `/impiegati` | ADMIN | Creates an employee (201) |
| PUT | `/impiegati/{id}` | ADMIN | Replaces an employee |
| PATCH | `/impiegati/{id}` | ADMIN | Updates only the fields that are sent |
| DELETE | `/impiegati/{id}` | ADMIN | Deletes an employee |

Request body for `POST` and `PUT` (the salary cannot be sent, it is always computed):

```json
{
  "name": "Luigi",
  "cognome": "Bianchi",
  "email": "luigi@mail.it",
  "age": 28,
  "tipoImpiegato": "IMPIEGATO",
  "dataAssunzione": "2023-05-15"
}
```

`tipoImpiegato` is optional and defaults to `IMPIEGATO`. `dataAssunzione` uses the format `yyyy-MM-dd`.

Employee response (illustrative values, they depend on the current date):

```json
{
  "id": 1,
  "name": "Mario",
  "cognome": "Rossi",
  "email": "mario.rossi@mail.it",
  "age": 35,
  "tipoImpiegato": "IMPIEGATO",
  "dataAssunzione": "2015-03-10",
  "anniAzienda": 11,
  "scattoAnzianita": "Scatto +20% (11–20 anni)",
  "salary": 38400.0,
  "stipendioMedioCategoria": 32000.0,
  "bonusAnnuale": 3100.0,
  "bonusEligibile": true,
  "anniMinimiBonus": 3,
  "percentualeBonus": 0.08
}
```

Error handling: validation errors return 400 with a readable message, and a missing employee returns 404 with a message.

### Salary Analytics (internal)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/salary/calcola` | Receives name, surname, age, email, computed salary, category, years of service and category base salary; returns the salary sheet |

The salary sheet contains: `employeeName`, `age`, `roleCategory`, `ralLorda`, `mensileLordo`, `irpefScaglione1`, `irpefScaglione2`, `irpefScaglione3`, `irpefTotaleAnnuo`, `irpefMensile`, `detrazioniLavoroDipendente`, `inpsAnnuo`, `inpsMensile`, `nettoAnnuo`, `nettoMensile`, `aliquotaEffettiva`, `salaryScore` and `salaryBenchmark`.

## Security

- Stateless authentication: the server keeps no session. A JWT signed with HS256 is issued at login and validated on every request by `JwtAuthFilter`.
- The token lifetime is 24 hours (`jwt.expiration`, in milliseconds). The signing key comes from `jwt.secret` and should be changed outside demos (the `JWT_SECRET` environment variable overrides it).
- Passwords are stored as BCrypt hashes.
- Permissions are defined in `SecurityConfig`: all `GET` requests on `/impiegati/**` need a logged-in user, while `POST`, `PUT`, `PATCH` and `DELETE` need `ADMIN`. The `/auth/utenti`, `/auth/promuovi` and `/auth/retrocedi` endpoints are protected with `@PreAuthorize("hasRole('ADMIN')")`.
- Public paths: `/auth/register`, `/auth/login`, `/`, `/login.html`, `/index.html` and `/logo.jpg`.
- Requests without a valid token receive 401 (browser page requests are redirected to `/login.html`).

## Web interface

The interface is a single-page application served by MS1 from `src/main/resources/static/`. Opening `http://localhost:8080` redirects to `login.html`, which also offers registration. After login the token and role are kept in the browser (`localStorage`) and sent with every request.

| Panel | Role |
|-------|------|
| Employee list | USER, ADMIN |
| Search by ID | USER, ADMIN |
| Search by surname | USER, ADMIN |
| Salary analysis | USER, ADMIN |
| Create employee | ADMIN |
| Replace employee (PUT) | ADMIN |
| Update employee (PATCH) | ADMIN |
| Delete employee | ADMIN |
| Users and permissions (promote, demote) | ADMIN |

Write, delete and permission menus are visible only to administrators. HTTP errors (400, 401, 403, 404, 409, 500, 502, 503) are translated into readable toast notifications.

## Running the project

### Option 1: Docker with a bundled database (recommended)

Requirements: Docker with Docker Compose.

```bash
docker compose -f docker-compose.autonomo.yml up --build
```

This builds both services, starts MySQL 8.3 (data and tables created automatically) and starts MS1 and MS2. Then open `http://localhost:8080` and sign in with a demo account.

| Service | URL / port |
|---------|------------|
| Employee Service (MS1) and web interface | `http://localhost:8080` |
| Salary Analytics (MS2) | `http://localhost:8081` |
| MySQL (from the host) | `127.0.0.1:3307`, database `employeedb` |

Stop the containers keeping the data with `docker compose -f docker-compose.autonomo.yml down`. To delete the database volume and start from scratch, add `-v`. The sample data is inserted only when the tables are empty.

### Option 2: Docker with other Compose files

| File | Description |
|------|-------------|
| `docker-compose.yml` | Uses the images published on GitHub Container Registry and a MySQL container. It needs access to those images |
| `docker-compose.casa.yml` | Builds locally and connects to an external MySQL server (home environment, profile `casa`) |
| `docker-compose.scuola.yml` | Builds locally and connects to an external MySQL server (school environment, profile `scuola`) |

Use the same command as above with the chosen file. For the two external-database files, set the host, database name and credentials of your own MySQL server in the file first.

### Option 3: Without Docker

Requirements: JDK 17 and a running MySQL server with an empty database.

```bash
# 1. Salary Analytics (port 8081)
cd salary-analytics
./mvnw spring-boot:run

# 2. Employee Service (port 8080), in a second terminal
cd employee-service
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/employeedb" \
SPRING_DATASOURCE_USERNAME="your_user" \
SPRING_DATASOURCE_PASSWORD="your_password" \
./mvnw spring-boot:run
```

The environment variables override the database settings of the default `casa` profile. On Windows use `mvnw.cmd` and set the variables with `set`.

## Configuration

Employee Service reads these properties, which can be overridden with environment variables (for example `JWT_SECRET`, `SALARY_SERVICE_URL`, `SPRING_DATASOURCE_URL`):

| Property | Default | Meaning |
|----------|---------|---------|
| `server.port` | `8080` | Port of MS1 |
| `spring.profiles.active` | `casa` | Active profile (`casa`, `scuola`, or `docker` in the Compose files) |
| `spring.datasource.*` | from the profile file | URL, user and password of MySQL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management |
| `jwt.secret` | demo value | Key used to sign the tokens |
| `jwt.expiration` | `86400000` | Token lifetime in milliseconds |
| `salary.service.url` | `http://localhost:8081` | Address of MS2 |

Salary Analytics only needs `server.port=8081`.

## Continuous integration

The workflow `.github/workflows/docker-publish.yml` runs on every push to the `main` branch. It logs in to GitHub Container Registry and builds and pushes two images: `ghcr.io/<owner>/employee-service:latest` and `ghcr.io/<owner>/salary-analytics:latest`. Both Dockerfiles use a multi-stage build (Maven with JDK 17 to compile, a JRE 17 image to run) and run the application as a non-root user.

## Known limitations

This is a learning project and is not hardened for production:

- The salary analysis panel of the interface reads the bonus fields from the response of `/impiegati/{id}/salary`, but that response does not include them, so the bonus block always shows "not yet earned". The bonus is correct in the employee list and in the create and update results.
- The API address is hard-coded as `http://localhost:8080` in `index.html`, so the interface works only when opened from the same machine that runs MS1.
- Demo accounts, the default JWT secret and the database credentials of the Compose files are written in the source and must be changed for any real use.
- The registration endpoint accepts any password and does not enforce rules on it.
- `JwtAuthFilter` prints the `Authorization` header to the console, which exposes tokens in the logs.
- CORS is open to every origin and MS2 has no authentication of its own; its port 8081 is published by the Compose files.
- Any unexpected runtime error in the employee controller, including an unreachable Salary Analytics service, is returned as a 404 by the same handler used for "employee not found".
- Automated tests are disabled in the build (`skipTests` in the MS1 `pom.xml`).
- The tax model is a simplified 2024 model for educational purposes.

---

<a id="italiano"></a>

# Italiano

## Indice

1. [Panoramica](#panoramica)
2. [Architettura](#architettura)
3. [Funzionalità](#funzionalità)
4. [Tecnologie](#tecnologie)
5. [Struttura del progetto](#struttura-del-progetto)
6. [Modello dei dati](#modello-dei-dati)
7. [Regole di stipendio e bonus](#regole-di-stipendio-e-bonus)
8. [Calcolo fiscale](#calcolo-fiscale)
9. [API REST](#api-rest)
10. [Sicurezza](#sicurezza)
11. [Interfaccia web](#interfaccia-web)
12. [Esecuzione del progetto](#esecuzione-del-progetto)
13. [Configurazione](#configurazione)
14. [Integrazione continua](#integrazione-continua)
15. [Limiti noti](#limiti-noti)

## Panoramica

Employee Manager è un gestionale composto da due servizi Spring Boot indipendenti che comunicano tra loro via HTTP:

- **Employee Service (MS1, porta 8080)** gestisce impiegati, utenti e autenticazione, serve l'interfaccia web e salva i dati su MySQL.
- **Salary Analytics (MS2, porta 8081)** riceve i dati di un impiegato e restituisce una scheda stipendiale completa: IRPEF, contributi INPS, detrazioni, netto e un punteggio stipendiale.

Gli impiegati non hanno uno stipendio salvato. Lo stipendio è sempre calcolato a runtime in base alla categoria e all'anzianità dell'impiegato, e il bonus annuale deriva dagli stessi dati. Il browser parla solo con MS1; MS2 viene chiamato da MS1.

Autore: Riccardo Croce.

## Architettura

```
Browser (login.html, index.html)
        |
        |  HTTP + JWT (Authorization: Bearer ...)
        v
+---------------------------+   POST /salary/calcola   +---------------------------+
|  MS1  Employee Service    | -----------------------> |  MS2  Salary Analytics    |
|  porta 8080               |                          |  porta 8081               |
|                           | <---- SalaryResponse --- |                           |
|  - CRUD impiegati         |                          |  - IRPEF (3 scaglioni)    |
|  - login / utenti / ruoli |                          |  - detrazioni lavoro      |
|  - stipendio e bonus      |                          |  - INPS                   |
|  - serve l'interfaccia    |                          |  - netto, salary score    |
+-------------+-------------+                          +---------------------------+
              |
              |  JPA / Hibernate
              v
        +-----------+
        |   MySQL   |
        +-----------+
```

MS1 raggiunge MS2 tramite l'URL impostato in `salary.service.url` (`http://localhost:8081` in locale, `http://ms2-salary:8081` nella rete Docker).

## Funzionalità

**Gestione degli impiegati**
- Elenco di tutti gli impiegati, ricerca per ID (nel percorso o in query string) e ricerca per cognome.
- Creazione, sostituzione completa (PUT), aggiornamento parziale (PATCH) ed eliminazione.
- Validazione con messaggi leggibili: età maggiore di zero, data di assunzione obbligatoria e non futura, coppia nome e cognome unica, email unica.
- Stipendio, scatto di anzianità e bonus annuale calcolati a runtime per ogni impiegato.

**Analisi dello stipendio**
- Scheda stipendiale su richiesta per qualsiasi impiegato: lordo annuo e mensile, IRPEF per scaglione, detrazioni, INPS, netto annuo e mensile, aliquota effettiva, salary score e benchmark.

**Autenticazione e autorizzazione**
- Registrazione e login, JWT senza stato, password cifrate con BCrypt.
- Due ruoli: `USER` (sola lettura) e `ADMIN` (accesso completo).
- Gli amministratori possono vedere gli utenti e promuoverli o retrocederli.

**Interfaccia web**
- Applicazione a pagina singola in HTML, CSS e JavaScript puro, con pagina di login e registrazione.
- I menu si adattano al ruolo dell'utente; gli errori compaiono come notifiche (toast) con messaggi comprensibili.

**Distribuzione**
- Immagini Docker per entrambi i servizi, quattro file Docker Compose per ambienti diversi e un workflow GitHub Actions che pubblica le immagini.

## Tecnologie

| Ambito | Tecnologia |
|--------|------------|
| Linguaggio | Java 17 |
| Framework | Spring Boot 3.2.5 (Web, Data JPA, Security) |
| Autenticazione | JSON Web Token (jjwt 0.11.5), BCrypt |
| Database | MySQL 8.3 (Docker) oppure un server MySQL esterno |
| Build | Maven (Maven Wrapper incluso in entrambi i servizi) |
| Front end | HTML5, CSS3, JavaScript puro |
| Container | Docker (build multi-stage), Docker Compose |
| CI | GitHub Actions, GitHub Container Registry |

## Struttura del progetto

```
microservices-project/
├── .github/workflows/
│   └── docker-publish.yml           Costruisce e pubblica le immagini Docker
├── docker-compose.yml               Immagini già pubblicate + container MySQL
├── docker-compose.autonomo.yml      Build locale + container MySQL (consigliato)
├── docker-compose.casa.yml          Build locale + MySQL esterno (ambiente casa)
├── docker-compose.scuola.yml        Build locale + MySQL esterno (ambiente scuola)
├── init.sql                         Inizializzazione del database (profilo autonomo)
├── ComeUtilizzareDocker.txt         Istruzioni Docker brevi
├── Documentazione_Progetto_Microservizi.md   Documentazione del progetto
│
├── employee-service/                MS1
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/employee_service/
│       │   ├── controller/          EmployeeController, AuthController, WelcomeController
│       │   ├── service/             EmployeeService, JwtService, UserDetailsServiceImpl
│       │   ├── security/            SecurityConfig, JwtAuthFilter
│       │   ├── entity/              Employee, SalaryRange, User
│       │   ├── repository/          EmployeeRepo, SalaryRangeRepository, UserRepository
│       │   ├── dto/                 EmployeeRequest, EmployeeResponse, SalaryResponse
│       │   ├── client/              SalaryClient (client HTTP verso MS2)
│       │   └── config/              DataLoader (dati di esempio), EmployeeConfig
│       └── resources/
│           ├── application.properties, application-casa.properties,
│           │   application-scuola.properties
│           └── static/              login.html, index.html, logo.jpg
│
└── salary-analytics/                MS2
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/salary_analytics/
        ├── SalaryController.java    POST /salary/calcola
        ├── SalaryService.java       IRPEF, detrazioni, INPS, score
        ├── SalaryRequest.java
        └── SalaryResponse.java
```

## Modello dei dati

**`employee`**

| Colonna | Tipo | Note |
|---------|------|------|
| `id` | INT, PK | Auto increment |
| `name`, `cognome` | VARCHAR(100) | Nome e cognome; la coppia deve essere unica (controllo nel service) |
| `email` | VARCHAR(150) | Univoca |
| `age` | INT | Deve essere maggiore di zero |
| `tipo_impiegato` | VARCHAR(20) | `OPERAIO`, `IMPIEGATO`, `QUADRO` o `DIRIGENTE`; predefinito `IMPIEGATO` |
| `data_assunzione` | DATE | Data di assunzione, obbligatoria e non futura |

**`salary_range`** è una tabella di lookup con una riga per categoria (una categoria ha molti impiegati).

| Colonna | Tipo | Note |
|---------|------|------|
| `tipo_impiegato` | VARCHAR(20), PK | Categoria |
| `stipendio_medio` | DOUBLE | Stipendio lordo annuo base della categoria |
| `descrizione` | VARCHAR(100) | Descrizione |
| `percentuale_bonus` | DOUBLE | Percentuale di bonus (0.08 significa 8%) |
| `anni_minimi_bonus` | INT | Anni di anzianità necessari per maturare il bonus |

**`users`**

| Colonna | Tipo | Note |
|---------|------|------|
| `id` | INT, PK | Auto increment |
| `username` | VARCHAR(100) | Univoco |
| `password` | VARCHAR(255) | Hash BCrypt |
| `ruolo` | VARCHAR(20) | `USER` oppure `ADMIN` |

Lo schema viene creato da Hibernate (`ddl-auto=update`) e, nel profilo Docker autonomo, anche da `init.sql`. Al primo avvio il `DataLoader` inserisce i dati di esempio se le tabelle sono vuote: 4 categorie salariali, 8 impiegati e 4 utenti.

| Account di prova | Password | Ruolo |
|------------------|----------|-------|
| `croceadmin` | `beeswarm` | ADMIN |
| `user1`, `user2`, `user3` | `pass1`, `pass2`, `pass3` | USER |

Questi account servono solo per la dimostrazione. I nuovi account creati dalla pagina di registrazione hanno sempre il ruolo `USER`.

## Regole di stipendio e bonus

Lo stipendio non viene mai salvato. Per ogni risposta MS1 lo calcola così:

```
stipendio = stipendio base della categoria x moltiplicatore di anzianità      (arrotondato ai 100 euro più vicini)
```

| Categoria | Stipendio lordo base | Percentuale bonus | Anni necessari per il bonus |
|-----------|---------------------:|------------------:|----------------------------:|
| `OPERAIO` | 24.000 | 5% | 3 |
| `IMPIEGATO` | 32.000 | 8% | 3 |
| `QUADRO` | 52.000 | 12% | 5 |
| `DIRIGENTE` | 90.000 | 20% | 5 |

| Anni di servizio | Moltiplicatore | Descrizione |
|------------------|---------------:|-------------|
| da 0 a 2 | 1,00 | Nessuno scatto |
| da 3 a 5 | 1,05 | +5% |
| da 6 a 10 | 1,12 | +12% |
| da 11 a 20 | 1,20 | +20% |
| oltre 20 | 1,30 | +30% |

Il **bonus annuale** è `stipendio x percentuale bonus`, arrotondato ai 100 euro più vicini. Viene riconosciuto solo quando gli anni di servizio raggiungono il minimo della categoria, altrimenti vale `0`. Gli anni di servizio sono gli anni interi tra la data di assunzione e oggi, quindi i valori cambiano nel tempo.

## Calcolo fiscale

Salary Analytics applica un modello semplificato delle regole italiane 2024 allo stipendio lordo annuo ricevuto da MS1.

- **Scaglioni IRPEF:** 23% fino a 28.000 euro, 35% da 28.001 a 50.000 euro, 43% oltre 50.000 euro.
- **Detrazione per lavoro dipendente** (art. 13 TUIR): detrazione decrescente che si azzera oltre 50.000 euro. L'IRPEF restituita in `irpefTotaleAnnuo` è l'importo dopo la detrazione.
- **INPS:** 9,19% dello stipendio, fino a un massimale di 119.650 euro.
- **Netto:** lordo meno IRPEF netta meno INPS. I valori mensili sono divisi per **13** mensilità.
- **Aliquota effettiva:** (IRPEF netta + INPS) divisi per il lordo.
- **Salary score (da 0 a 100):** curva logistica centrata sulla retribuzione media italiana di circa 29.000 euro l'anno; 50 significa in linea con la media. Il testo del benchmark è "Sotto la media nazionale" (sotto 40), "In linea con la media nazionale" (da 40 a 59), "Sopra la media nazionale" (da 60 a 79) e "Top 20% nazionale" (da 80 in su).

Il modello ha scopo didattico e non è pensato per una vera elaborazione dei cedolini.

## API REST

Tutti i percorsi sono serviti da MS1 sulla porta 8080. Gli endpoint protetti richiedono l'header `Authorization: Bearer <token>`.

### Autenticazione e utenti

| Metodo | Percorso | Ruolo | Descrizione |
|--------|----------|-------|-------------|
| POST | `/auth/register` | pubblico | Registra un utente con ruolo `USER`. Body: `{"username": "...", "password": "..."}`. Restituisce 201, oppure 400 se lo username esiste |
| POST | `/auth/login` | pubblico | Restituisce `{"token", "ruolo", "username"}` |
| GET | `/auth/utenti` | ADMIN | Elenca tutti gli utenti (id, username, ruolo) |
| PATCH | `/auth/promuovi/{id}` | ADMIN | Promuove un utente a `ADMIN` |
| PATCH | `/auth/retrocedi/{id}` | ADMIN | Retrocede un utente a `USER` |

### Impiegati

| Metodo | Percorso | Ruolo | Descrizione |
|--------|----------|-------|-------------|
| GET | `/impiegati` | USER, ADMIN | Elenca tutti gli impiegati |
| GET | `/impiegati/id/{id}` | USER, ADMIN | Cerca un impiegato per ID |
| GET | `/impiegati/cerca?id={id}` | USER, ADMIN | Cerca un impiegato per ID (query string) |
| GET | `/impiegati/cognome/{cognome}` | USER, ADMIN | Cerca per cognome (distingue maiuscole e minuscole) |
| GET | `/impiegati/{id}/salary` | USER, ADMIN | Scheda stipendiale calcolata da MS2 |
| POST | `/impiegati` | ADMIN | Crea un impiegato (201) |
| PUT | `/impiegati/{id}` | ADMIN | Sostituisce un impiegato |
| PATCH | `/impiegati/{id}` | ADMIN | Aggiorna solo i campi inviati |
| DELETE | `/impiegati/{id}` | ADMIN | Elimina un impiegato |

Body per `POST` e `PUT` (lo stipendio non si può inviare, è sempre calcolato):

```json
{
  "name": "Luigi",
  "cognome": "Bianchi",
  "email": "luigi@mail.it",
  "age": 28,
  "tipoImpiegato": "IMPIEGATO",
  "dataAssunzione": "2023-05-15"
}
```

`tipoImpiegato` è facoltativo e vale `IMPIEGATO` se omesso. `dataAssunzione` usa il formato `yyyy-MM-dd`.

Risposta per un impiegato (valori indicativi, dipendono dalla data corrente):

```json
{
  "id": 1,
  "name": "Mario",
  "cognome": "Rossi",
  "email": "mario.rossi@mail.it",
  "age": 35,
  "tipoImpiegato": "IMPIEGATO",
  "dataAssunzione": "2015-03-10",
  "anniAzienda": 11,
  "scattoAnzianita": "Scatto +20% (11–20 anni)",
  "salary": 38400.0,
  "stipendioMedioCategoria": 32000.0,
  "bonusAnnuale": 3100.0,
  "bonusEligibile": true,
  "anniMinimiBonus": 3,
  "percentualeBonus": 0.08
}
```

Gestione degli errori: gli errori di validazione restituiscono 400 con un messaggio leggibile, un impiegato inesistente restituisce 404 con un messaggio.

### Salary Analytics (interno)

| Metodo | Percorso | Descrizione |
|--------|----------|-------------|
| POST | `/salary/calcola` | Riceve nome, cognome, età, email, stipendio calcolato, categoria, anni di servizio e stipendio base della categoria; restituisce la scheda stipendiale |

La scheda contiene: `employeeName`, `age`, `roleCategory`, `ralLorda`, `mensileLordo`, `irpefScaglione1`, `irpefScaglione2`, `irpefScaglione3`, `irpefTotaleAnnuo`, `irpefMensile`, `detrazioniLavoroDipendente`, `inpsAnnuo`, `inpsMensile`, `nettoAnnuo`, `nettoMensile`, `aliquotaEffettiva`, `salaryScore` e `salaryBenchmark`.

## Sicurezza

- Autenticazione senza stato: il server non mantiene sessioni. Al login viene emesso un JWT firmato con HS256, validato a ogni richiesta da `JwtAuthFilter`.
- Il token dura 24 ore (`jwt.expiration`, in millisecondi). La chiave di firma viene da `jwt.secret` e fuori dalle dimostrazioni va cambiata (la variabile d'ambiente `JWT_SECRET` la sostituisce).
- Le password sono salvate come hash BCrypt.
- I permessi sono definiti in `SecurityConfig`: tutte le richieste `GET` su `/impiegati/**` richiedono un utente autenticato, mentre `POST`, `PUT`, `PATCH` e `DELETE` richiedono `ADMIN`. Gli endpoint `/auth/utenti`, `/auth/promuovi` e `/auth/retrocedi` sono protetti con `@PreAuthorize("hasRole('ADMIN')")`.
- Percorsi pubblici: `/auth/register`, `/auth/login`, `/`, `/login.html`, `/index.html` e `/logo.jpg`.
- Le richieste senza token valido ricevono 401 (le richieste di pagine dal browser vengono reindirizzate a `/login.html`).

## Interfaccia web

L'interfaccia è un'applicazione a pagina singola servita da MS1 da `src/main/resources/static/`. Aprendo `http://localhost:8080` si viene reindirizzati a `login.html`, che offre anche la registrazione. Dopo il login token e ruolo sono conservati nel browser (`localStorage`) e inviati a ogni richiesta.

| Pannello | Ruolo |
|----------|-------|
| Elenco impiegati | USER, ADMIN |
| Ricerca per ID | USER, ADMIN |
| Ricerca per cognome | USER, ADMIN |
| Analisi stipendio | USER, ADMIN |
| Crea impiegato | ADMIN |
| Sostituisci impiegato (PUT) | ADMIN |
| Aggiorna impiegato (PATCH) | ADMIN |
| Elimina impiegato | ADMIN |
| Utenti e permessi (promuovi, retrocedi) | ADMIN |

I menu di scrittura, eliminazione e permessi sono visibili solo agli amministratori. Gli errori HTTP (400, 401, 403, 404, 409, 500, 502, 503) vengono tradotti in notifiche (toast) leggibili.

## Esecuzione del progetto

### Opzione 1: Docker con database incluso (consigliata)

Requisiti: Docker con Docker Compose.

```bash
docker compose -f docker-compose.autonomo.yml up --build
```

Il comando costruisce i due servizi, avvia MySQL 8.3 (dati e tabelle creati automaticamente) e avvia MS1 e MS2. Poi apri `http://localhost:8080` ed entra con un account di prova.

| Servizio | URL / porta |
|----------|-------------|
| Employee Service (MS1) e interfaccia web | `http://localhost:8080` |
| Salary Analytics (MS2) | `http://localhost:8081` |
| MySQL (dal computer host) | `127.0.0.1:3307`, database `employeedb` |

Per fermare i container mantenendo i dati usa `docker compose -f docker-compose.autonomo.yml down`. Per cancellare anche il volume del database e ripartire da zero aggiungi `-v`. I dati di esempio vengono inseriti solo se le tabelle sono vuote.

### Opzione 2: Docker con gli altri file Compose

| File | Descrizione |
|------|-------------|
| `docker-compose.yml` | Usa le immagini pubblicate su GitHub Container Registry e un container MySQL. Serve l'accesso a quelle immagini |
| `docker-compose.casa.yml` | Costruisce in locale e si collega a un server MySQL esterno (ambiente casa, profilo `casa`) |
| `docker-compose.scuola.yml` | Costruisce in locale e si collega a un server MySQL esterno (ambiente scuola, profilo `scuola`) |

Usa lo stesso comando di sopra con il file scelto. Per i due file con database esterno, imposta prima nel file host, nome del database e credenziali del tuo server MySQL.

### Opzione 3: Senza Docker

Requisiti: JDK 17 e un server MySQL in esecuzione con un database vuoto.

```bash
# 1. Salary Analytics (porta 8081)
cd salary-analytics
./mvnw spring-boot:run

# 2. Employee Service (porta 8080), in un secondo terminale
cd employee-service
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/employeedb" \
SPRING_DATASOURCE_USERNAME="tuo_utente" \
SPRING_DATASOURCE_PASSWORD="tua_password" \
./mvnw spring-boot:run
```

Le variabili d'ambiente sostituiscono le impostazioni del database del profilo `casa`, attivo per impostazione predefinita. Su Windows usa `mvnw.cmd` e imposta le variabili con `set`.

## Configurazione

Employee Service legge queste proprietà, che si possono sostituire con variabili d'ambiente (per esempio `JWT_SECRET`, `SALARY_SERVICE_URL`, `SPRING_DATASOURCE_URL`):

| Proprietà | Valore predefinito | Significato |
|-----------|--------------------|-------------|
| `server.port` | `8080` | Porta di MS1 |
| `spring.profiles.active` | `casa` | Profilo attivo (`casa`, `scuola`, oppure `docker` nei file Compose) |
| `spring.datasource.*` | dal file del profilo | URL, utente e password di MySQL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Gestione dello schema |
| `jwt.secret` | valore di prova | Chiave per firmare i token |
| `jwt.expiration` | `86400000` | Durata del token in millisecondi |
| `salary.service.url` | `http://localhost:8081` | Indirizzo di MS2 |

Salary Analytics richiede solo `server.port=8081`.

## Integrazione continua

Il workflow `.github/workflows/docker-publish.yml` parte a ogni push sul ramo `main`. Esegue l'accesso a GitHub Container Registry e costruisce e pubblica due immagini: `ghcr.io/<owner>/employee-service:latest` e `ghcr.io/<owner>/salary-analytics:latest`. Entrambi i Dockerfile usano una build multi-stage (Maven con JDK 17 per compilare, un'immagine JRE 17 per eseguire) e avviano l'applicazione con un utente non root.

## Limiti noti

Il progetto ha scopo didattico e non è predisposto per la produzione:

- Il pannello di analisi stipendio dell'interfaccia legge i campi del bonus dalla risposta di `/impiegati/{id}/salary`, ma quella risposta non li contiene, quindi il blocco bonus mostra sempre "non ancora maturato". Il bonus è corretto nell'elenco impiegati e nei risultati di creazione e aggiornamento.
- L'indirizzo dell'API è scritto nel codice come `http://localhost:8080` in `index.html`, quindi l'interfaccia funziona solo se aperta dallo stesso computer su cui gira MS1.
- Gli account di prova, il segreto JWT predefinito e le credenziali del database nei file Compose sono scritti nel codice e vanno cambiati per qualsiasi uso reale.
- L'endpoint di registrazione accetta qualsiasi password e non impone regole.
- `JwtAuthFilter` stampa l'header `Authorization` sulla console, quindi i token finiscono nei log.
- CORS è aperto a tutte le origini e MS2 non ha autenticazione propria; la sua porta 8081 è pubblicata dai file Compose.
- Qualsiasi errore imprevisto a runtime nel controller degli impiegati, compreso un servizio Salary Analytics non raggiungibile, viene restituito come 404 dallo stesso gestore usato per "impiegato non trovato".
- I test automatici sono disattivati nella build (`skipTests` nel `pom.xml` di MS1).
- Il modello fiscale è una versione semplificata delle regole 2024, a scopo didattico.
