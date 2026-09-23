package com.example.employee_service.config;

import com.example.employee_service.entity.Employee;
import com.example.employee_service.entity.Employee.TipoImpiegato;
import com.example.employee_service.entity.SalaryRange;
import com.example.employee_service.entity.User;
import com.example.employee_service.repository.EmployeeRepo;
import com.example.employee_service.repository.SalaryRangeRepository;
import com.example.employee_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

/**
 * DataLoader — inserisce dati di esempio all'avvio se le tabelle sono vuote.
 *
 * Gli impiegati NON hanno un salary salvato: lo stipendio viene sempre
 * calcolato a runtime da EmployeeService in base a tipo + anzianità.
 */
@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(EmployeeRepo empRepo,
                                      SalaryRangeRepository salaryRangeRepo,
                                      UserRepository userRepo,
                                      PasswordEncoder encoder) {
        return args -> {

            // ── salary_range (lookup) ─────────────────────────────────────
            // Colonne: tipo, stipendioMedio, descrizione, percentualeBonus, anniMinimiBonus
            //
            // Logica bonus:
            //   OPERAIO    →  5% dopo 3 anni
            //   IMPIEGATO  →  8% dopo 3 anni
            //   QUADRO     → 12% dopo 5 anni
            //   DIRIGENTE  → 20% dopo 5 anni
            if (salaryRangeRepo.count() == 0) {
                System.out.println("[DataLoader] Inserimento salary_range...");
                salaryRangeRepo.saveAll(List.of(
                        creaSalaryRange("OPERAIO",   24000, "Lavoratori manuali / produzione",             0.05, 3),
                        creaSalaryRange("IMPIEGATO", 32000, "Personale amministrativo / tecnico",           0.08, 3),
                        creaSalaryRange("QUADRO",    52000, "Responsabili intermedi / specialisti senior",  0.12, 5),
                        creaSalaryRange("DIRIGENTE", 90000, "Dirigenti / top management",                   0.20, 5)
                ));
                System.out.println("[DataLoader] 4 categorie salariali inserite.");
            }

            // ── Dipendenti ────────────────────────────────────────────────
            // Nota: nessun salary manuale — tutto calcolato a runtime
            if (empRepo.count() == 0) {
                System.out.println("[DataLoader] Inserimento dipendenti...");
                empRepo.saveAll(List.of(
                        creaEmp("Mario",      "Rossi",    "mario.rossi@mail.it",    35, TipoImpiegato.IMPIEGATO, LocalDate.of(2015, 3, 10)),
                        creaEmp("Laura",      "Bianchi",  "laura.bianchi@mail.it",  28, TipoImpiegato.IMPIEGATO, LocalDate.of(2022, 9,  1)),
                        creaEmp("Giovanni",   "Verdi",    "giovanni.verdi@mail.it", 42, TipoImpiegato.QUADRO,    LocalDate.of(2008, 6, 15)),
                        creaEmp("Sofia",      "Ferrari",  "sofia.ferrari@mail.it",  31, TipoImpiegato.IMPIEGATO, LocalDate.of(2019, 1, 20)),
                        creaEmp("Alessandro", "Conti",    "ale.conti@mail.it",      24, TipoImpiegato.OPERAIO,   LocalDate.of(2024, 2,  5)),
                        creaEmp("Chiara",     "Russo",    "chiara.russo@mail.it",   38, TipoImpiegato.QUADRO,    LocalDate.of(2012, 4, 18)),
                        creaEmp("Luca",       "Esposito", "luca.esposito@mail.it",  27, TipoImpiegato.OPERAIO,   LocalDate.of(2021, 7, 12)),
                        creaEmp("Martina",    "Romano",   "martina.romano@mail.it", 45, TipoImpiegato.DIRIGENTE, LocalDate.of(2005, 11, 3))
                ));
                System.out.println("[DataLoader] 8 dipendenti inseriti.");
            } else {
                System.out.println("[DataLoader] Dipendenti già presenti, skip.");
            }

            // ── Utenti ────────────────────────────────────────────────────
            if (userRepo.count() == 0) {
                System.out.println("[DataLoader] Inserimento utenti...");
                userRepo.saveAll(List.of(
                        creaUser("croceadmin", "beeswarm", "ADMIN", encoder),
                        creaUser("user1",      "pass1",    "USER",  encoder),
                        creaUser("user2",      "pass2",    "USER",  encoder),
                        creaUser("user3",      "pass3",    "USER",  encoder)
                ));
                System.out.println("[DataLoader] 4 utenti inseriti (1 admin + 3 user).");
            } else {
                System.out.println("[DataLoader] Utenti già presenti, skip.");
            }
        };
    }

    private SalaryRange creaSalaryRange(String tipo, double media, String desc,
                                        double percentualeBonus, int anniMinimiBonus) {
        SalaryRange sr = new SalaryRange();
        sr.setTipoImpiegato(tipo);
        sr.setStipendioMedio(media);
        sr.setDescrizione(desc);
        sr.setPercentualeBonus(percentualeBonus);
        sr.setAnniMinimiBonus(anniMinimiBonus);
        return sr;
    }

    private Employee creaEmp(String name, String cognome, String email, int age,
                             TipoImpiegato tipo, LocalDate assunzione) {
        Employee e = new Employee();
        e.setName(name);
        e.setCognome(cognome);
        e.setEmail(email);
        e.setAge(age);
        e.setTipoImpiegato(tipo);
        e.setDataAssunzione(assunzione);
        // salary NON impostato: calcolato sempre a runtime
        return e;
    }

    private User creaUser(String username, String password, String ruolo, PasswordEncoder enc) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(enc.encode(password));
        u.setRuolo(ruolo);
        return u;
    }
}
