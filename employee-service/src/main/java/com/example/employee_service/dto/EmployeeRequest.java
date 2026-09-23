package com.example.employee_service.dto;

import com.example.employee_service.entity.Employee.TipoImpiegato;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/**
 * DTO per la creazione / aggiornamento di un impiegato.
 *
 * Lo stipendio NON è un campo accettato in input: viene calcolato
 * automaticamente dal service in base a tipoImpiegato e anzianità.
 */
public class EmployeeRequest {

    private String name;
    private String cognome;
    private String email;
    private int age;

    /** Categoria: OPERAIO | IMPIEGATO | QUADRO | DIRIGENTE */
    private TipoImpiegato tipoImpiegato;

    /** Data assunzione nel formato yyyy-MM-dd */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAssunzione;

    // ── Getters & Setters ────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public TipoImpiegato getTipoImpiegato() { return tipoImpiegato; }
    public void setTipoImpiegato(TipoImpiegato tipoImpiegato) { this.tipoImpiegato = tipoImpiegato; }

    public LocalDate getDataAssunzione() { return dataAssunzione; }
    public void setDataAssunzione(LocalDate dataAssunzione) { this.dataAssunzione = dataAssunzione; }
}
