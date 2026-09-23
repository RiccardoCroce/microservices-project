package com.example.salary_analytics;

import com.example.salary_analytics.SalaryRequest;
import com.example.salary_analytics.SalaryResponse;
import com.example.salary_analytics.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/salary")
@CrossOrigin(origins = "*")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    /**
     * POST /salary/calcola
     * Body: { "name": "Mario", "cognome": "Rossi", "age": 35, "email": "..." }
     * Restituisce la scheda stipendiale completa.
     */
    @PostMapping("/calcola")
    public ResponseEntity<SalaryResponse> calcolaStipendio(@RequestBody SalaryRequest request) {
        SalaryResponse response = salaryService.calcola(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
