package com.example.employee_service.dto;

import com.example.employee_service.entity.Employee.TipoImpiegato;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/**
 * DTO di risposta per un impiegato.
 *
 * Lo stipendio è sempre calcolato a runtime (mai letto dal DB):
 *   salary = stipendioMedioCategoria × moltiplicatoreAnzianità
 *
 * Il bonus è separato dallo stipendio: è un importo annuale aggiuntivo
 * che matura solo dopo un numero minimo di anni in azienda.
 */
public class EmployeeResponse {

    private int id;
    private String name;
    private String cognome;
    private String email;
    private int age;
    private TipoImpiegato tipoImpiegato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAssunzione;

    /** Anni di anzianità calcolati a runtime */
    private int anniAzienda;

    /** Descrizione testuale dello scatto di anzianità attivo */
    private String scattoAnzianita;

    /** RAL lorda calcolata: stipendioMedioCategoria × moltiplicatoreAnzianità */
    private Double salary;

    /** Stipendio medio della categoria (dalla tabella salary_range) */
    private Double stipendioMedioCategoria;

    /**
     * Bonus annuale spettante.
     * 0.0 se l'impiegato non ha ancora raggiunto la soglia minima di anzianità.
     */
    private Double bonusAnnuale;

    /** true se il dipendente ha maturato il diritto al bonus */
    private boolean bonusEligibile;

    /** Anni minimi per avere diritto al bonus (dalla salary_range della categoria) */
    private int anniMinimiBonus;

    /** Percentuale di bonus della categoria (es. 0.08 = 8%) */
    private double percentualeBonus;

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

    public int getAnniAzienda() { return anniAzienda; }
    public void setAnniAzienda(int anniAzienda) { this.anniAzienda = anniAzienda; }

    public String getScattoAnzianita() { return scattoAnzianita; }
    public void setScattoAnzianita(String scattoAnzianita) { this.scattoAnzianita = scattoAnzianita; }

    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }

    public Double getStipendioMedioCategoria() { return stipendioMedioCategoria; }
    public void setStipendioMedioCategoria(Double stipendioMedioCategoria) { this.stipendioMedioCategoria = stipendioMedioCategoria; }

    public Double getBonusAnnuale() { return bonusAnnuale; }
    public void setBonusAnnuale(Double bonusAnnuale) { this.bonusAnnuale = bonusAnnuale; }

    public boolean isBonusEligibile() { return bonusEligibile; }
    public void setBonusEligibile(boolean bonusEligibile) { this.bonusEligibile = bonusEligibile; }

    public int getAnniMinimiBonus() { return anniMinimiBonus; }
    public void setAnniMinimiBonus(int anniMinimiBonus) { this.anniMinimiBonus = anniMinimiBonus; }

    public double getPercentualeBonus() { return percentualeBonus; }
    public void setPercentualeBonus(double percentualeBonus) { this.percentualeBonus = percentualeBonus; }
}
