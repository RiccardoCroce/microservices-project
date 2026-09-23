-- ─────────────────────────────────────────────────────────────
-- Script di inizializzazione automatica del database
-- Eseguito da MySQL SOLO al primo avvio (volume vuoto)
-- ─────────────────────────────────────────────────────────────

GRANT ALL PRIVILEGES ON employeedb.* TO 'appuser'@'%';
FLUSH PRIVILEGES;

-- Tabella lookup: stipendi medi e parametri bonus per categoria.
-- NON è 1:1 con employee — ogni categoria può avere N impiegati.
CREATE TABLE IF NOT EXISTS salary_range (
    tipo_impiegato     VARCHAR(20)  PRIMARY KEY,
    stipendio_medio    DOUBLE       NOT NULL,   -- RAL base lorda annua (€)
    descrizione        VARCHAR(100),
    percentuale_bonus  DOUBLE       NOT NULL DEFAULT 0.0,  -- es. 0.08 = 8%
    anni_minimi_bonus  INT          NOT NULL DEFAULT 3     -- anni per maturare il bonus
);

-- I dati della salary_range vengono inseriti dal DataLoader Java all'avvio.
-- Valori di default:
--   OPERAIO    24.000 →  5% dopo 3 anni
--   IMPIEGATO  32.000 →  8% dopo 3 anni
--   QUADRO     52.000 → 12% dopo 5 anni
--   DIRIGENTE  90.000 → 20% dopo 5 anni

-- Tabella employee: NO colonna salary.
-- Lo stipendio è sempre calcolato a runtime:
--   salary = salary_range.stipendio_medio × moltiplicatore_anzianità
-- Il bonus è calcolato separatamente se anni_azienda >= anni_minimi_bonus.
CREATE TABLE IF NOT EXISTS employee (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    cognome         VARCHAR(100) NOT NULL,
    email           VARCHAR(150) UNIQUE,
    age             INT,
    tipo_impiegato  VARCHAR(20)  NOT NULL DEFAULT 'IMPIEGATO',
    data_assunzione DATE         NOT NULL,
    CONSTRAINT fk_tipo FOREIGN KEY (tipo_impiegato) REFERENCES salary_range(tipo_impiegato)
);

CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255)        NOT NULL,
    ruolo    VARCHAR(20)         NOT NULL DEFAULT 'USER'
);

-- I dati di esempio vengono inseriti dal DataLoader Java all'avvio di Spring
