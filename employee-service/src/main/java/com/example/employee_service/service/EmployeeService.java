package com.example.employee_service.service;

import com.example.employee_service.entity.Employee;
import com.example.employee_service.entity.Employee.TipoImpiegato;
import com.example.employee_service.entity.SalaryRange;
import com.example.employee_service.repository.EmployeeRepo;
import com.example.employee_service.repository.SalaryRangeRepository;
import com.example.employee_service.dto.EmployeeRequest;
import com.example.employee_service.dto.EmployeeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SalaryRangeRepository salaryRangeRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // LOGICA DI CALCOLO STIPENDIO
    //
    // Lo stipendio NON è memorizzato in tabella employee.
    // Viene sempre calcolato a runtime come:
    //
    //   stipendio = stipendioMedioCategoria × moltiplicatoreAnzianità
    //
    // Scatti di anzianità:
    //   0–2 anni   → ×1.00  (neoassunto)
    //   3–5 anni   → ×1.05  (+5%)
    //   6–10 anni  → ×1.12  (+12%)
    //   11–20 anni → ×1.20  (+20%)
    //   > 20 anni  → ×1.30  (+30%)
    //
    // Basi salariali (dalla tabella salary_range):
    //   OPERAIO    → 24.000 €
    //   IMPIEGATO  → 32.000 €
    //   QUADRO     → 52.000 €
    //   DIRIGENTE  → 90.000 €
    //
    // Risultato arrotondato ai 100 € più vicini.
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // LOGICA DI CALCOLO BONUS
    //
    // Bonus annuale = stipendioEffettivo × percentualeBonus (da salary_range)
    //
    // Condizione: l'impiegato deve avere almeno 'anniMinimiBonus' anni in azienda.
    //
    //   OPERAIO    →  5% dopo 3 anni
    //   IMPIEGATO  →  8% dopo 3 anni
    //   QUADRO     → 12% dopo 5 anni
    //   DIRIGENTE  → 20% dopo 5 anni
    //
    // Arrotondato ai 100 € più vicini.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcola il numero di anni interi trascorsi dalla data di assunzione a oggi.
     */
    public int calcolaAnniAzienda(LocalDate dataAssunzione) {
        if (dataAssunzione == null) return 0;
        return Period.between(dataAssunzione, LocalDate.now()).getYears();
    }

    /**
     * Restituisce il moltiplicatore di anzianità in base agli anni lavorati.
     */
    private double moltiplicatoreAnzianita(int anni) {
        if (anni < 3)  return 1.00;
        if (anni < 6)  return 1.05;
        if (anni < 11) return 1.12;
        if (anni < 21) return 1.20;
        return                1.30;
    }

    /**
     * Testo descrittivo dello scatto di anzianità corrente.
     */
    private String descrizioneScatto(int anni) {
        if (anni < 3)  return "Nessuno scatto (< 3 anni)";
        if (anni < 6)  return "Scatto +5% (3–5 anni)";
        if (anni < 11) return "Scatto +12% (6–10 anni)";
        if (anni < 21) return "Scatto +20% (11–20 anni)";
        return                "Scatto +30% (> 20 anni — Veterano)";
    }

    /**
     * Calcola la RAL effettiva di un impiegato.
     * Sempre derivata da: stipendioMedioCategoria × moltiplicatoreAnzianità.
     * Risultato arrotondato ai 100 € più vicini.
     */
    public double calcolaStipendioEffettivo(Employee emp) {
        String tipoKey = emp.getTipoImpiegato() != null
                ? emp.getTipoImpiegato().name()
                : TipoImpiegato.IMPIEGATO.name();

        double base = salaryRangeRepo.findById(tipoKey)
                .map(SalaryRange::getStipendioMedio)
                .orElse(32_000.0);   // fallback IMPIEGATO se la categoria non esiste

        int anni = calcolaAnniAzienda(emp.getDataAssunzione());
        double stipendio = base * moltiplicatoreAnzianita(anni);

        // Arrotonda a 100 €
        return Math.round(stipendio / 100.0) * 100.0;
    }

    /**
     * Calcola il bonus annuale spettante all'impiegato.
     *
     *  - Recupera percentualeBonus e anniMinimiBonus dalla salary_range della categoria
     *  - Se anni in azienda >= anniMinimiBonus → bonus = stipendioEffettivo * percentualeBonus
     *  - Altrimenti → 0.0
     *
     * @param emp       l'entità impiegato
     * @param stipendio lo stipendio effettivo già calcolato (evita doppio calcolo)
     * @return importo bonus in euro (arrotondato a 100 €), oppure 0.0
     */
    public double calcolaBonus(Employee emp, double stipendio) {
        if (emp.getTipoImpiegato() == null) return 0.0;

        Optional<SalaryRange> srOpt = salaryRangeRepo.findById(emp.getTipoImpiegato().name());
        if (srOpt.isEmpty()) return 0.0;

        SalaryRange sr = srOpt.get();
        int anni = calcolaAnniAzienda(emp.getDataAssunzione());

        if (anni < sr.getAnniMinimiBonus()) return 0.0;

        double bonus = stipendio * sr.getPercentualeBonus();
        return Math.round(bonus / 100.0) * 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING ENTITY → DTO
    // ─────────────────────────────────────────────────────────────────────────

    private EmployeeResponse toResponse(Employee emp) {
        EmployeeResponse r = new EmployeeResponse();
        r.setId(emp.getId());
        r.setName(emp.getName());
        r.setCognome(emp.getCognome());
        r.setEmail(emp.getEmail());
        r.setAge(emp.getAge());
        r.setTipoImpiegato(emp.getTipoImpiegato());
        r.setDataAssunzione(emp.getDataAssunzione());

        int anni = calcolaAnniAzienda(emp.getDataAssunzione());
        r.setAnniAzienda(anni);
        r.setScattoAnzianita(descrizioneScatto(anni));

        double stipendio = calcolaStipendioEffettivo(emp);
        r.setSalary(stipendio);

        double bonus = calcolaBonus(emp, stipendio);
        r.setBonusAnnuale(bonus);
        r.setBonusEligibile(bonus > 0.0);

        if (emp.getTipoImpiegato() != null) {
            salaryRangeRepo.findById(emp.getTipoImpiegato().name())
                    .ifPresent(sr -> {
                        r.setStipendioMedioCategoria(sr.getStipendioMedio());
                        r.setAnniMinimiBonus(sr.getAnniMinimiBonus());
                        r.setPercentualeBonus(sr.getPercentualeBonus());
                    });
        }
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public EmployeeResponse getEmployeeById(int id) {
        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Impiegato con id " + id + " non trovato"));
        return toResponse(emp);
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByCognome(String cognome) {
        return employeeRepo.findByCognome(cognome).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (request.getAge() <= 0)
            throw new IllegalArgumentException("L'età deve essere maggiore di 0");
        if (request.getDataAssunzione() == null)
            throw new IllegalArgumentException("La data di assunzione è obbligatoria");
        if (request.getDataAssunzione().isAfter(LocalDate.now()))
            throw new IllegalArgumentException("La data di assunzione non può essere futura");
        if (employeeRepo.existsByNameAndCognome(request.getName(), request.getCognome()))
            throw new IllegalArgumentException("Esiste già un impiegato con nome '"
                    + request.getName() + "' e cognome '" + request.getCognome() + "'");
        if (request.getEmail() != null && employeeRepo.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("L'email '" + request.getEmail() + "' è già registrata");

        Employee emp = fromRequest(request, new Employee());
        return toResponse(employeeRepo.save(emp));
    }

    public EmployeeResponse replaceEmployee(int id, EmployeeRequest request) {
        if (!employeeRepo.existsById(id))
            throw new RuntimeException("Impiegato con id " + id + " non trovato");
        if (request.getAge() <= 0)
            throw new IllegalArgumentException("L'età deve essere maggiore di 0");
        if (request.getDataAssunzione() == null)
            throw new IllegalArgumentException("La data di assunzione è obbligatoria");
        if (employeeRepo.existsByNameAndCognomeAndIdNot(request.getName(), request.getCognome(), id))
            throw new IllegalArgumentException("Esiste già un altro impiegato con nome '"
                    + request.getName() + "' e cognome '" + request.getCognome() + "'");
        if (request.getEmail() != null && employeeRepo.existsByEmailAndIdNot(request.getEmail(), id))
            throw new IllegalArgumentException("L'email '" + request.getEmail() + "' è già registrata da un altro impiegato");

        Employee emp = fromRequest(request, new Employee());
        emp.setId(id);
        return toResponse(employeeRepo.save(emp));
    }

    public EmployeeResponse updateEmployee(int id, EmployeeRequest request) {
        Employee emp = employeeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Impiegato con id " + id + " non trovato"));

        if (request.getAge() > 0) emp.setAge(request.getAge());
        else if (request.getAge() < 0) throw new IllegalArgumentException("L'età non può essere negativa");

        if (request.getName() != null)    emp.setName(request.getName());
        if (request.getCognome() != null) emp.setCognome(request.getCognome());
        if (request.getTipoImpiegato() != null) emp.setTipoImpiegato(request.getTipoImpiegato());
        if (request.getDataAssunzione() != null) {
            if (request.getDataAssunzione().isAfter(LocalDate.now()))
                throw new IllegalArgumentException("La data di assunzione non può essere futura");
            emp.setDataAssunzione(request.getDataAssunzione());
        }

        if (employeeRepo.existsByNameAndCognomeAndIdNot(emp.getName(), emp.getCognome(), id))
            throw new IllegalArgumentException("Esiste già un altro impiegato con nome '"
                    + emp.getName() + "' e cognome '" + emp.getCognome() + "'");
        if (request.getEmail() != null) {
            if (employeeRepo.existsByEmailAndIdNot(request.getEmail(), id))
                throw new IllegalArgumentException("L'email '" + request.getEmail() + "' è già registrata da un altro impiegato");
            emp.setEmail(request.getEmail());
        }

        return toResponse(employeeRepo.save(emp));
    }

    public String deleteEmployee(int id) {
        if (!employeeRepo.existsById(id))
            throw new RuntimeException("Impiegato con id " + id + " non trovato");
        employeeRepo.deleteById(id);
        return "Impiegato con id " + id + " eliminato con successo";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Employee fromRequest(EmployeeRequest req, Employee emp) {
        emp.setName(req.getName());
        emp.setCognome(req.getCognome());
        emp.setEmail(req.getEmail());
        emp.setAge(req.getAge());
        emp.setTipoImpiegato(req.getTipoImpiegato() != null
                ? req.getTipoImpiegato()
                : TipoImpiegato.IMPIEGATO);
        emp.setDataAssunzione(req.getDataAssunzione());
        // Nessun salary da salvare: tutto calcolato a runtime
        return emp;
    }
}
