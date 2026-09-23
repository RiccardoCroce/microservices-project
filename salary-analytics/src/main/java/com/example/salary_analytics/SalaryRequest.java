package com.example.salary_analytics;

public class SalaryRequest {

    private String name;
    private String cognome;
    private int age;
    private String email;
    private double salary;

    /** Categoria contrattuale: OPERAIO | IMPIEGATO | QUADRO | DIRIGENTE */
    private String tipoImpiegato;

    /** Anni interi lavorati nell'azienda (calcolati da MS1) */
    private int anniAzienda;

    /** Stipendio medio della categoria (da salary_range, fornito da MS1) */
    private double stipendioMedioCategoria;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCognome() { return cognome; }
    public void setCognome(String cognome) { this.cognome = cognome; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getTipoImpiegato() { return tipoImpiegato; }
    public void setTipoImpiegato(String tipoImpiegato) { this.tipoImpiegato = tipoImpiegato; }

    public int getAnniAzienda() { return anniAzienda; }
    public void setAnniAzienda(int anniAzienda) { this.anniAzienda = anniAzienda; }

    public double getStipendioMedioCategoria() { return stipendioMedioCategoria; }
    public void setStipendioMedioCategoria(double stipendioMedioCategoria) { this.stipendioMedioCategoria = stipendioMedioCategoria; }
}