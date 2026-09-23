package com.example.employee_service.entity;

import jakarta.persistence.*;

/**
 * Tabella lookup: stipendi medi e parametri bonus per categoria contrattuale.
 * Non è 1:1 con Employee — ogni categoria può avere N impiegati.
 */
@Entity
@Table(name = "salary_range")
public class SalaryRange {

    @Id
    @Column(name = "tipo_impiegato", length = 20)
    private String tipoImpiegato;

    @Column(name = "stipendio_medio", nullable = false)
    private double stipendioMedio;

    @Column(name = "descrizione")
    private String descrizione;

    /**
     * Percentuale di bonus erogabile (es. 0.10 = 10% dello stipendio effettivo).
     * Il bonus viene applicato solo se l'impiegato ha almeno 'anniMinimiBonus' anni
     * di anzianità nell'azienda.
     */
    @Column(name = "percentuale_bonus", nullable = false)
    private double percentualeBonus;

    /**
     * Numero minimo di anni in azienda per avere diritto al bonus.
     */
    @Column(name = "anni_minimi_bonus", nullable = false)
    private int anniMinimiBonus;

    // ── Getters & Setters ────────────────────────────────────────

    public String getTipoImpiegato() { return tipoImpiegato; }
    public void setTipoImpiegato(String tipoImpiegato) { this.tipoImpiegato = tipoImpiegato; }

    public double getStipendioMedio() { return stipendioMedio; }
    public void setStipendioMedio(double stipendioMedio) { this.stipendioMedio = stipendioMedio; }

    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public double getPercentualeBonus() { return percentualeBonus; }
    public void setPercentualeBonus(double percentualeBonus) { this.percentualeBonus = percentualeBonus; }

    public int getAnniMinimiBonus() { return anniMinimiBonus; }
    public void setAnniMinimiBonus(int anniMinimiBonus) { this.anniMinimiBonus = anniMinimiBonus; }
}
