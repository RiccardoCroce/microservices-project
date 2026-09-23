package com.example.employee_service.client;

import com.example.employee_service.dto.SalaryResponse;
import com.example.employee_service.entity.Employee;
import com.example.employee_service.repository.SalaryRangeRepository;
import com.example.employee_service.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Client RestClient che chiama MS2 (SalaryAnalyticsService) sulla porta 8081.
 *
 * Il salary inviato a MS2 è SEMPRE quello calcolato a runtime da EmployeeService
 * (stipendioMedioCategoria × moltiplicatoreAnzianità), mai letto dal DB.
 * Vengono inviati anche tipoImpiegato, anniAzienda e stipendioMedioCategoria
 * così MS2 può arricchire la risposta con informazioni categoriali e sul bonus.
 */
@Component
public class SalaryClient {

    private final RestClient restClient;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SalaryRangeRepository salaryRangeRepo;

    public SalaryClient(@Value("${salary.service.url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Invia i dati dell'impiegato a MS2 e riceve la scheda stipendiale completa.
     * Lo stipendio è calcolato qui prima dell'invio (mai preso dal DB).
     */
    public SalaryResponse getSalaryAnalysis(Employee employee) {
        // Calcola stipendio e anni a runtime
        double stipendioCalcolato = employeeService.calcolaStipendioEffettivo(employee);
        int anniAzienda = employeeService.calcolaAnniAzienda(employee.getDataAssunzione());

        double stipendioMedioCategoria = salaryRangeRepo
                .findById(employee.getTipoImpiegato() != null
                        ? employee.getTipoImpiegato().name() : "IMPIEGATO")
                .map(sr -> sr.getStipendioMedio())
                .orElse(32_000.0);

        Map<String, Object> body = new HashMap<>();
        body.put("name",                    employee.getName());
        body.put("cognome",                 employee.getCognome());
        body.put("age",                     employee.getAge());
        body.put("email",                   employee.getEmail() != null ? employee.getEmail() : "");
        body.put("salary",                  stipendioCalcolato);           // calcolato, non dal DB
        body.put("tipoImpiegato",           employee.getTipoImpiegato() != null
                                                ? employee.getTipoImpiegato().name() : "IMPIEGATO");
        body.put("anniAzienda",             anniAzienda);
        body.put("stipendioMedioCategoria", stipendioMedioCategoria);

        return restClient.post()
                .uri("/salary/calcola")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(SalaryResponse.class);
    }
}
