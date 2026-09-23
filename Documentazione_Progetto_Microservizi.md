# Documentazione — Progetto Microservizi

**Autore:** Riccardo Croce  
**Stack:** Java 21 · Spring Boot 3 · MySQL · Docker · JWT  
**Ultima revisione:** Maggio 2026

---

## Indice

1. [Panoramica architetturale](#1-panoramica-architetturale)
2. [Microservizio 1 — Employee Service](#2-microservizio-1--employee-service)
   - [Entità e modello dati](#21-entità-e-modello-dati)
   - [Sicurezza e autenticazione JWT](#22-sicurezza-e-autenticazione-jwt)
   - [API REST — Impiegati](#23-api-rest--impiegati)
   - [API REST — Autenticazione e utenti](#24-api-rest--autenticazione-e-utenti)
3. [Microservizio 2 — Salary Analytics](#3-microservizio-2--salary-analytics)
   - [Logica di calcolo fiscale](#31-logica-di-calcolo-fiscale)
   - [API REST](#32-api-rest)
4. [Comunicazione tra microservizi](#4-comunicazione-tra-microservizi)
5. [Frontend — Employee Manager](#5-frontend--employee-manager)
   - [Autenticazione](#51-autenticazione)
   - [Pannelli e funzionalità](#52-pannelli-e-funzionalità)
   - [Gestione degli errori](#53-gestione-degli-errori)
6. [Database](#6-database)
7. [Deploy con Docker](#7-deploy-con-docker)
8. [Configurazione e profili Spring](#8-configurazione-e-profili-spring)
9. [Struttura del progetto](#9-struttura-del-progetto)

---

## 1. Panoramica architetturale

Il sistema è composto da **due microservizi Spring Boot indipendenti** che comunicano via HTTP:

```
Browser / Frontend (index.html)
         │
         │ HTTP + JWT
         ▼
┌─────────────────────────┐         HTTP (POST)       ┌─────────────────────────┐
│   MS1 — Employee        │ ────────────────────────► │   MS2 — Salary          │
│   Service  :8080        │                           │   Analytics  :8081      │
│                         │ ◄──── SalaryResponse ──── │                         │
│  - CRUD impiegati       │                           │  - Calcolo IRPEF        │
│  - Auth JWT (login)     │                           │  - Calcolo INPS         │
│  - Gestione utenti      │                           │  - Salary Score         │
│  - Serve il frontend    │                           │  - Detrazione lavoro    │
└──────────┬──────────────┘                           └─────────────────────────┘
           │
           │ JPA / Hibernate
           ▼
     ┌──────────┐
     │  MySQL   │
     │ :3306    │
     └──────────┘
```

MS1 è l'unico punto di accesso per il browser. MS2 non espone nulla al frontend: riceve richieste solo da MS1.

---

## 2. Microservizio 1 — Employee Service

**Porta:** `8080`  
**Percorso sorgenti:** `employee-service/`

### 2.1 Entità e modello dati

#### Employee

| Campo     | Tipo      | Note                        |
|-----------|-----------|-----------------------------|
| `id`      | `int` (PK)| Auto-increment              |
| `name`    | `String`  | Nome                        |
| `cognome` | `String`  | Cognome                     |
| `email`   | `String`  | Univoca (UNIQUE)            |
| `age`     | `int`     | Età — deve essere > 0       |
| `salary`  | `Double`  | Stipendio annuo — deve essere > 0 |

Vincoli di unicità gestiti nel service: non possono esistere due impiegati con lo stesso `name + cognome`, né due con la stessa `email`.

#### User

| Campo      | Tipo     | Note                              |
|------------|----------|-----------------------------------|
| `id`       | `int` (PK)| Auto-increment                   |
| `username` | `String` | Univoco                           |
| `password` | `String` | BCrypt hash                       |
| `ruolo`    | `String` | Valori: `USER`, `ADMIN`           |

### 2.2 Sicurezza e autenticazione JWT

Il sistema usa **Spring Security** in modalità stateless con **JWT (JSON Web Token)**.

**Flusso di autenticazione:**

1. Il client invia `POST /auth/login` con `username` e `password`.
2. Spring autentica le credenziali via `AuthenticationManager`.
3. Se valide, `JwtService` genera un token firmato con la chiave segreta.
4. Il token viene restituito al client insieme a `ruolo` e `username`.
5. Per ogni richiesta successiva il client include `Authorization: Bearer <token>`.
6. `JwtAuthFilter` valida il token ad ogni richiesta e popola il `SecurityContext`.

**Matrice dei permessi:**

| Operazione                | USER | ADMIN |
|---------------------------|:----:|:-----:|
| GET `/impiegati/**`       | ✔    | ✔     |
| POST `/impiegati/**`      | ✘    | ✔     |
| PUT `/impiegati/**`       | ✘    | ✔     |
| PATCH `/impiegati/**`     | ✘    | ✔     |
| DELETE `/impiegati/**`    | ✘    | ✔     |
| GET `/auth/utenti`        | ✘    | ✔     |
| PATCH `/auth/promuovi`    | ✘    | ✔     |
| PATCH `/auth/retrocedi`   | ✘    | ✔     |
| POST `/auth/login`        | pub  | pub   |
| POST `/auth/register`     | pub  | pub   |

**Configurazione JWT:**

| Parametro          | Valore default                                |
|--------------------|-----------------------------------------------|
| `jwt.secret`       | `chiaveSuperSegretaLungaAlmeno32Caratteri`    |
| `jwt.expiration`   | `86400000` ms (24 ore)                        |

### 2.3 API REST — Impiegati

Base path: `/impiegati`

---

#### GET `/impiegati`
Restituisce la lista completa di tutti gli impiegati.

**Risposta (200 OK):**
```json
[
  { "id": 1, "name": "Mario", "cognome": "Rossi", "email": "mario@mail.it", "age": 35, "salary": 42000 },
  ...
]
```

---

#### GET `/impiegati/id/{id}`
Cerca un impiegato per ID.

**Parametri path:** `id` — intero

**Risposta (200 OK):** oggetto `EmployeeResponse`  
**Errore (500):** se l'ID non esiste — `RuntimeException` propagata

---

#### GET `/impiegati/cognome/{cognome}`
Cerca impiegati per cognome (case-sensitive).

**Parametri path:** `cognome` — stringa

**Risposta (200 OK):** array di `EmployeeResponse` (può essere vuoto)

---

#### GET `/impiegati/cerca?id={id}`
Alternativa alla ricerca per ID tramite query string.

**Parametri query:** `id` — intero

---

#### POST `/impiegati`
Crea un nuovo impiegato. Richiede ruolo **ADMIN**.

**Body (JSON):**
```json
{
  "name": "Luigi",
  "cognome": "Bianchi",
  "email": "luigi@mail.it",
  "age": 28,
  "salary": 32000
}
```

**Risposta (201 Created):** oggetto `EmployeeResponse`

**Errori (400 Bad Request):**
- Età ≤ 0
- Stipendio ≤ 0 o assente
- Nome + cognome già esistente
- Email già registrata

---

#### PUT `/impiegati/{id}`
Sostituisce completamente un impiegato esistente. Richiede ruolo **ADMIN**. Tutti i campi sono obbligatori.

**Risposta (200 OK):** oggetto `EmployeeResponse` aggiornato

**Errori (400):** stessi vincoli del POST + controlli unicità escludendo l'impiegato stesso.

---

#### PATCH `/impiegati/{id}`
Aggiornamento parziale. Richiede ruolo **ADMIN**. Inviare solo i campi da modificare.

**Risposta (200 OK):** oggetto `EmployeeResponse` aggiornato

---

#### DELETE `/impiegati/{id}`
Elimina definitivamente un impiegato. Richiede ruolo **ADMIN**.

**Risposta (200 OK):** stringa di conferma  
**Errore (500):** se l'ID non esiste

---

#### GET `/impiegati/{id}/salary`
Chiama MS2 per ottenere l'analisi stipendiale completa dell'impiegato.

**Risposta (200 OK):** oggetto `SalaryResponse` (vedi sezione MS2)

---

### 2.4 API REST — Autenticazione e utenti

Base path: `/auth`

---

#### POST `/auth/register`
Registra un nuovo utente. Il ruolo è sempre `USER`.

**Body:** `{ "username": "...", "password": "..." }`  
**Risposta (201 Created):** `"Utente registrato!"`  
**Errore (400):** username già esistente

---

#### POST `/auth/login`
Autentica un utente e restituisce il token JWT.

**Body:** `{ "username": "...", "password": "..." }`

**Risposta (200 OK):**
```json
{
  "token": "eyJ...",
  "ruolo": "ADMIN",
  "username": "admin"
}
```

---

#### GET `/auth/utenti` *(solo ADMIN)*
Restituisce la lista di tutti gli utenti registrati.

**Risposta (200 OK):**
```json
[
  { "id": 1, "username": "admin", "ruolo": "ADMIN" },
  { "id": 2, "username": "mario", "ruolo": "USER" }
]
```

---

#### PATCH `/auth/promuovi/{id}` *(solo ADMIN)*
Promuove un utente al ruolo `ADMIN`.

**Risposta (200 OK):** stringa di conferma

---

#### PATCH `/auth/retrocedi/{id}` *(solo ADMIN)*
Retrocede un utente al ruolo `USER`.

**Risposta (200 OK):** stringa di conferma

---

## 3. Microservizio 2 — Salary Analytics

**Porta:** `8081`  
**Percorso sorgenti:** `salary-analytics/`  
**Accesso:** solo da MS1, non esposto direttamente al browser.

### 3.1 Logica di calcolo fiscale

MS2 riceve i dati dell'impiegato e applica la normativa fiscale italiana 2024.

**IRPEF — Scaglioni 2024:**

| Scaglione | Aliquota |
|-----------|----------|
| Fino a € 28.000       | 23% |
| € 28.001 — € 50.000  | 35% |
| Oltre € 50.000        | 43% |

**Detrazioni per lavoro dipendente (art. 13 TUIR):**

| RAL                   | Detrazione                            |
|-----------------------|---------------------------------------|
| ≤ € 15.000            | Da € 1.955 fino a € 3.000 (proporzionale) |
| € 15.001 — € 28.000  | Progressiva decrescente da € 1.910   |
| € 28.001 — € 50.000  | Progressiva decrescente               |
| > € 50.000            | € 0                                   |

**INPS — Contributi dipendente:**  
Aliquota **9,19%** fino al massimale annuo (≈ € 119.650 per il 2024).

**Salary Score (0–100):**  
Curva logistica centrata sulla RAL media ISTAT italiana (≈ € 29.000/anno). Score 50 = in linea con la media nazionale.

**Mensilità:** tutti i valori mensili sono calcolati su **13 mensilità**.

### 3.2 API REST

#### POST `/salary/calcola`
Calcola la scheda stipendiale completa.

**Body (SalaryRequest):**
```json
{
  "name": "Mario",
  "cognome": "Rossi",
  "age": 35,
  "email": "mario@mail.it",
  "salary": 42000
}
```

**Risposta (200 OK — SalaryResponse):**
```json
{
  "employeeName": "Mario Rossi",
  "age": 35,
  "roleCategory": "Mid-Level",
  "ralLorda": 42000.0,
  "mensileLordo": 3230.77,
  "irpefScaglione1": 6440.0,
  "irpefScaglione2": 4900.0,
  "irpefScaglione3": 0.0,
  "irpefTotaleAnnuo": 9380.0,
  "irpefMensile": 721.54,
  "detrazioniLavoroDipendente": 636.36,
  "inpsAnnuo": 3859.8,
  "inpsMensile": 296.91,
  "nettoAnnuo": 28760.2,
  "nettoMensile": 2212.32,
  "aliquotaEffettiva": 31.57,
  "salaryScore": 67,
  "salaryBenchmark": "Sopra la media nazionale"
}
```

**Categoria ruolo per età:**

| Età        | Categoria         |
|------------|-------------------|
| < 25       | Junior            |
| 25–30      | Junior+           |
| 31–37      | Mid-Level         |
| 38–45      | Senior            |
| > 45       | Lead / Principal  |

---

## 4. Comunicazione tra microservizi

MS1 chiama MS2 tramite il `SalaryClient` (classe annotata con `@Component`):

- **Endpoint chiamato:** `POST {salary.service.url}/salary/calcola`
- **URL configurabile** via proprietà `salary.service.url`
  - In locale: `http://localhost:8081`
  - In Docker: `http://ms2-salary:8081` (rete Docker interna)

Il client costruisce un `SalaryRequest` dai dati dell'impiegato e deserializza la risposta in `SalaryResponse`.

---

## 5. Frontend — Employee Manager

Il frontend è una **Single Page Application** in HTML/CSS/JS puro, servita staticamente da MS1 (`src/main/resources/static/`).

### 5.1 Autenticazione

All'avvio (`window.onload`) il frontend controlla il `localStorage`:
- Se manca il token → redirect automatico a `/login.html`
- Se il token è presente → la pagina viene caricata con le sezioni appropriate al ruolo

Il token JWT viene inviato in ogni richiesta come header `Authorization: Bearer <token>`.

### 5.2 Pannelli e funzionalità

| Sezione              | Metodo | Endpoint                    | Ruolo richiesto |
|----------------------|--------|-----------------------------|-----------------|
| Lista impiegati      | GET    | `/impiegati`                | USER / ADMIN    |
| Cerca per ID         | GET    | `/impiegati/id/{id}`        | USER / ADMIN    |
| Cerca per cognome    | GET    | `/impiegati/cognome/{nome}` | USER / ADMIN    |
| Crea impiegato       | POST   | `/impiegati`                | ADMIN           |
| Sostituisci (PUT)    | PUT    | `/impiegati/{id}`           | ADMIN           |
| Aggiorna (PATCH)     | PATCH  | `/impiegati/{id}`           | ADMIN           |
| Elimina              | DELETE | `/impiegati/{id}`           | ADMIN           |
| Salary Analytics     | GET    | `/impiegati/{id}/salary`    | USER / ADMIN    |
| Gestione utenti      | GET/PATCH | `/auth/utenti`, `/auth/promuovi/{id}`, `/auth/retrocedi/{id}` | ADMIN |

I menu **Write**, **Delete** e **Permessi** sono visibili solo agli utenti con ruolo `ADMIN`.

### 5.3 Gestione degli errori

Tutti i messaggi di errore vengono mostrati tramite **toast notification** nell'angolo in basso a destra. La funzione `errorMsg(response, fallback)` traduce automaticamente i codici HTTP in messaggi comprensibili:

| Codice HTTP | Messaggio mostrato all'utente                                    |
|-------------|------------------------------------------------------------------|
| 400         | Il body della risposta del server (es. "L'email è già registrata") oppure "Dati non validi — controlla i campi inseriti" |
| 401         | Non autorizzato — effettua il login                              |
| 403         | Accesso negato — operazione riservata agli amministratori        |
| 404         | Risorsa non trovata                                              |
| 409         | Conflitto — elemento già esistente                               |
| 500         | Errore interno del server — riprova più tardi                    |
| 502         | Server non raggiungibile — verifica che i microservizi siano attivi |
| 503         | Servizio temporaneamente non disponibile                         |

Per i **400 Bad Request**, il backend restituisce nel corpo della risposta il messaggio di validazione specifico (es. `"L'email 'x@y.it' è già registrata"`): questo testo viene mostrato direttamente all'utente.

---

## 6. Database

**DBMS:** MySQL  
**Schema:** auto-generato da Hibernate (`ddl-auto=update`) oppure tramite `init.sql` in Docker.

**Tabelle:**

```sql
CREATE TABLE employee (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email   VARCHAR(150) UNIQUE,
    age     INT,
    salary  DOUBLE
);

CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,   -- BCrypt
    ruolo    VARCHAR(20)  NOT NULL DEFAULT 'USER'
);
```

I dati di esempio (impiegati e utenti iniziali) vengono inseriti all'avvio di Spring tramite la classe `DataLoader`.

---

## 7. Deploy con Docker

Sono disponibili tre file Docker Compose:

| File                          | Ambiente        | Database                             |
|-------------------------------|-----------------|--------------------------------------|
| `docker-compose.casa.yml`     | Sviluppo casa   | MySQL su host (`192.168.x.x:3306`)   |
| `docker-compose.scuola.yml`   | Sviluppo scuola | MySQL su host (IP scuola)            |
| `docker-compose.autonomo.yml` | Autonomo        | MySQL container incluso nel Compose  |

### Comandi principali

```bash
# Avvio (rebuild immagini)
docker-compose -f docker-compose.casa.yml up --build

# Avvio senza rebuild
docker-compose -f docker-compose.casa.yml up

# Stop e rimozione volumi
docker-compose -f docker-compose.casa.yml down -v
```

### Servizi Docker (esempio `casa`)

| Container      | Immagine        | Porta host | Porta container |
|----------------|-----------------|-----------|-----------------|
| `ms1-employee` | build locale    | 8080      | 8080            |
| `ms2-salary`   | build locale    | 8081      | 8081            |

MS1 dipende da MS2 (`depends_on: ms2-salary`). La comunicazione interna avviene tramite la rete Docker: MS1 raggiunge MS2 all'indirizzo `http://ms2-salary:8081`.

---

## 8. Configurazione e profili Spring

MS1 supporta **profili Spring** per ambienti diversi:

| File                                | Profilo attivo | Descrizione           |
|-------------------------------------|----------------|-----------------------|
| `application.properties`            | —              | Configurazione base   |
| `application-casa.properties`       | `casa`         | DB di casa            |
| `application-scuola.properties`     | `scuola`       | DB di scuola          |

**Proprietà chiave di MS1:**

```properties
server.port=8080
jwt.secret=chiaveSuperSegretaLungaAlmeno32Caratteri
jwt.expiration=86400000
salary.service.url=http://localhost:8081
spring.jpa.hibernate.ddl-auto=update
```

**Proprietà chiave di MS2:**

```properties
server.port=8081
spring.application.name=salary-analytics
```

In Docker le proprietà sensibili (URL database, credenziali, secret JWT) vengono iniettate come variabili d'ambiente nel `docker-compose.yml`.

---

## 9. Struttura del progetto

```
Progetto Microservizi/
│
├── employee-service/                  ← MS1
│   ├── src/main/java/.../
│   │   ├── controller/
│   │   │   ├── EmployeeController.java   GET/POST/PUT/PATCH/DELETE /impiegati
│   │   │   ├── AuthController.java       /auth/login, /auth/register, /auth/utenti...
│   │   │   └── WelcomeController.java
│   │   ├── service/
│   │   │   ├── EmployeeService.java      Logica CRUD + validazioni
│   │   │   ├── JwtService.java           Generazione e validazione token JWT
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── security/
│   │   │   ├── SecurityConfig.java       Configurazione Spring Security
│   │   │   └── JwtAuthFilter.java        Filtro HTTP per validare JWT
│   │   ├── entity/
│   │   │   ├── Employee.java
│   │   │   └── User.java
│   │   ├── repository/
│   │   │   ├── EmployeeRepo.java
│   │   │   └── UserRepository.java
│   │   ├── dto/
│   │   │   ├── EmployeeRequest.java
│   │   │   ├── EmployeeResponse.java
│   │   │   └── SalaryResponse.java
│   │   ├── client/
│   │   │   └── SalaryClient.java         Chiamate HTTP verso MS2
│   │   └── config/
│   │       ├── DataLoader.java            Dati iniziali al boot
│   │       └── EmployeeConfig.java        Bean ModelMapper
│   ├── src/main/resources/static/
│   │   ├── index.html                     Frontend SPA
│   │   ├── login.html
│   │   └── logo.jpg
│   └── Dockerfile
│
├── salary-analytics/                  ← MS2
│   ├── src/main/java/.../
│   │   ├── SalaryController.java         POST /salary/calcola
│   │   ├── SalaryService.java            Calcolo IRPEF, INPS, Salary Score
│   │   ├── SalaryRequest.java
│   │   └── SalaryResponse.java
│   └── Dockerfile
│
├── docker-compose.casa.yml
├── docker-compose.scuola.yml
├── docker-compose.autonomo.yml
├── init.sql                           Script DDL inizializzazione DB
└── ComeUtilizzareDocker.txt
```
