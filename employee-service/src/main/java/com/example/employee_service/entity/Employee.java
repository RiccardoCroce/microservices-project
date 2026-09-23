package com.example.employee_service.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {

    /**
     * Tipi di impiegato supportati.
     * Corrispondono ai valori nella tabella salary_range.
     */
    public enum TipoImpiegato {
        OPERAIO, IMPIEGATO, QUADRO, DIRIGENTE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "cognome")
    private String cognome;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "age")
    private int age;

    /** Categoria contrattuale dell'impiegato */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_impiegato", nullable = false)
    private TipoImpiegato tipoImpiegato = TipoImpiegato.IMPIEGATO;

    /**
     * Data di assunzione — serve per calcolare l'anzianità e il bonus.
     * Lo stipendio NON è memorizzato nella tabella: viene sempre
     * derivato da salary_range + scatti di anzianità a runtime.
     */
    @Column(name = "data_assunzione", nullable = false)
    private LocalDate dataAssunzione;

    // ── Getters & Setters ────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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
