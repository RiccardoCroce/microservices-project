package com.example.employee_service.controller;

import com.example.employee_service.client.SalaryClient;
import com.example.employee_service.dto.EmployeeRequest;
import com.example.employee_service.dto.EmployeeResponse;
import com.example.employee_service.dto.SalaryResponse;
import com.example.employee_service.entity.Employee;
import com.example.employee_service.service.EmployeeService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/impiegati")
public class EmployeeController {
    @Autowired
    private SalaryClient salaryClient;
    @Autowired
    private EmployeeService employeeService;

    // Gestione errori di validazione → 400 Bad Request con messaggio leggibile
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidation(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // FASE 1a - GET per ID: localhost:8080/impiegati/id/2
    @GetMapping("/id/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable("id") int id) {
        EmployeeResponse employee = employeeService.getEmployeeById(id);
        return ResponseEntity.status(HttpStatus.OK).body(employee);
    }

    // FASE 1b - GET elenco completo: localhost:8080/impiegati
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        List<EmployeeResponse> employees = employeeService.getAllEmployees();
        return ResponseEntity.status(HttpStatus.OK).body(employees);
    }

    // FASE 1c - GET per cognome: localhost:8080/impiegati/cognome/Rossi
    @GetMapping("/cognome/{cognome}")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByCognome(@PathVariable("cognome") String cognome) {
        List<EmployeeResponse> employees = employeeService.getEmployeesByCognome(cognome);
        return ResponseEntity.status(HttpStatus.OK).body(employees);
    }

    // FASE 1d - GET per query string: localhost:8080/impiegati/cerca?id=3
    @GetMapping("/cerca")
    public ResponseEntity<EmployeeResponse> getEmployeeByQueryId(@RequestParam("id") int id) {
        EmployeeResponse employee = employeeService.getEmployeeById(id);
        return ResponseEntity.status(HttpStatus.OK).body(employee);
    }

    // FASE 2 - POST: crea nuovo impiegato
    // Body JSON esempio: {"name":"Mario","cognome":"Bianchi","email":"mario@mail.it","age":"30"}
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // FASE 2 - PUT: sostituisce completamente un impiegato esistente
    // localhost:8080/impiegati/5
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> replaceEmployee(
            @PathVariable("id") int id,
            @RequestBody EmployeeRequest request) {
        EmployeeResponse updated = employeeService.replaceEmployee(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    // FASE 2 - PATCH: aggiorna parzialmente un impiegato
    // localhost:8080/impiegati/5
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable("id") int id,
            @RequestBody EmployeeRequest request) {
        EmployeeResponse updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    // FASE 2 - DELETE: elimina un impiegato
    // localhost:8080/impiegati/5
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable("id") int id) {
        String message = employeeService.deleteEmployee(id);
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }

    @GetMapping("/{id}/salary")
    public ResponseEntity<SalaryResponse> getSalaryAnalysis(@PathVariable("id") int id) {
        EmployeeResponse empResp = employeeService.getEmployeeById(id);

        Employee emp = new Employee();
        emp.setName(empResp.getName());
        emp.setCognome(empResp.getCognome());
        emp.setAge(empResp.getAge());
        emp.setEmail(empResp.getEmail());
        emp.setTipoImpiegato(empResp.getTipoImpiegato());
        emp.setDataAssunzione(empResp.getDataAssunzione());

        SalaryResponse salary = salaryClient.getSalaryAnalysis(emp);
        return ResponseEntity.status(HttpStatus.OK).body(salary);
    }
}
