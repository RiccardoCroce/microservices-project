package com.example.salary_analytics;

import org.springframework.stereotype.Service;

@Service
public class SalaryService {

    // ─────────────────────────────────────────────────────────────────────────
    // IRPEF 2024 — scaglioni reali:
    //   0      – 28.000 → 23%
    //   28.001 – 50.000 → 35%
    //   > 50.000        → 43%
    // ─────────────────────────────────────────────────────────────────────────
    private double[] calcolaIrpef(double ral) {
        double s1 = 0, s2 = 0, s3 = 0;

        if (ral <= 28_000) {
            s1 = ral * 0.23;
        } else if (ral <= 50_000) {
            s1 = 28_000 * 0.23;
            s2 = (ral - 28_000) * 0.35;
        } else {
            s1 = 28_000 * 0.23;
            s2 = (50_000 - 28_000) * 0.35;
            s3 = (ral - 50_000) * 0.43;
        }

        return new double[]{ round2(s1), round2(s2), round2(s3) };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detrazione per lavoro dipendente (art. 13 TUIR)
    // ─────────────────────────────────────────────────────────────────────────
    private double calcolaDetrazione(double ral) {
        if (ral <= 15_000) {
            double q = 690 + 1_380 * ((15_000 - ral) / 15_000);
            return round2(Math.min(1_955 + q, 3_000));
        } else if (ral <= 28_000) {
            return round2(1_910 * ((28_000 - ral) / 13_000) + 978);
        } else if (ral <= 50_000) {
            return round2(1_910 * ((50_000 - ral) / 22_000));
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INPS lavoratore dipendente: 9,19% (IVS + disoccupazione)
    // Massimale pensionistico 2024 ≈ 119.650 €
    // ─────────────────────────────────────────────────────────────────────────
    private double calcolaInps(double ral) {
        double massimale = 119_650;
        return round2(Math.min(ral, massimale) * 0.0919);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Salary Score 0–100 rispetto alla media ISTAT ~29.000 €/anno (2024).
    // Curva logistica: score 50 = media esatta.
    // ─────────────────────────────────────────────────────────────────────────
    private int calcolaSalaryScore(double ral) {
        double ratio = ral / 29_000.0;
        double score = 100 / (1 + Math.exp(-3.5 * (ratio - 1)));
        return (int) Math.round(Math.min(100, Math.max(0, score)));
    }

    private String salaryBenchmark(int score) {
        if (score < 40) return "Sotto la media nazionale";
        if (score < 60) return "In linea con la media nazionale";
        if (score < 80) return "Sopra la media nazionale";
        return "Top 20% nazionale";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Descrizione testuale dello scatto di anzianità
    // ─────────────────────────────────────────────────────────────────────────
    private String descrizioneScatto(int anni) {
        if (anni < 3)  return "Nessuno scatto (< 3 anni)";
        if (anni < 6)  return "Scatto +5% (3–5 anni)";
        if (anni < 11) return "Scatto +12% (6–10 anni)";
        if (anni < 21) return "Scatto +20% (11–20 anni)";
        return                "Scatto +30% (> 20 anni — Veterano)";
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    public SalaryResponse calcola(SalaryRequest req) {

        // RAL calcolata da MS1 (stipendioMedioCategoria × moltiplicatoreAnzianità)
        double ral = req.getSalary();

        double[] irpef       = calcolaIrpef(ral);
        double irpefTotale   = irpef[0] + irpef[1] + irpef[2];
        double detrazione    = calcolaDetrazione(ral);
        double irpefNetta    = Math.max(0, irpefTotale - detrazione);
        double inpsAnnuo     = calcolaInps(ral);
        double nettoAnnuo    = round2(ral - irpefNetta - inpsAnnuo);

        double stipMedio     = req.getStipendioMedioCategoria();
        double delta         = round2(ral - stipMedio);

        int score            = calcolaSalaryScore(ral);

        SalaryResponse r = new SalaryResponse();
        r.setEmployeeName(req.getName() + " " + req.getCognome());
        r.setAge(req.getAge());
        r.setRoleCategory(req.getTipoImpiegato() != null ? req.getTipoImpiegato() : "N/D");
        r.setAnniAzienda(req.getAnniAzienda());
        r.setScattoAnzianita(descrizioneScatto(req.getAnniAzienda()));

        r.setRalLorda(ral);
        r.setStipendioMedioCategoria(stipMedio);
        r.setDeltaCategoriale(delta);
        r.setMensileLordo(round2(ral / 13.0));   // 13 mensilità

        r.setIrpefScaglione1(irpef[0]);
        r.setIrpefScaglione2(irpef[1]);
        r.setIrpefScaglione3(irpef[2]);
        r.setIrpefTotaleAnnuo(round2(irpefNetta));
        r.setIrpefMensile(round2(irpefNetta / 13.0));

        r.setDetrazioniLavoroDipendente(detrazione);

        r.setInpsAnnuo(inpsAnnuo);
        r.setInpsMensile(round2(inpsAnnuo / 13.0));

        r.setNettoAnnuo(nettoAnnuo);
        r.setNettoMensile(round2(nettoAnnuo / 13.0));

        r.setAliquotaEffettiva(round2((irpefNetta + inpsAnnuo) / ral * 100));
        r.setSalaryScore(score);
        r.setSalaryBenchmark(salaryBenchmark(score));

        return r;
    }
}
