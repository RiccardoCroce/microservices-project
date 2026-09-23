package com.example.employee_service.dto;


public class SalaryResponse {

    private String employeeName;
    private int age;
    private String roleCategory;

    private double ralLorda;
    private double mensileLordo;

    private double irpefScaglione1;
    private double irpefScaglione2;
    private double irpefScaglione3;
    private double irpefTotaleAnnuo;
    private double irpefMensile;

    private double detrazioniLavoroDipendente;

    private double inpsAnnuo;
    private double inpsMensile;

    private double nettoAnnuo;
    private double nettoMensile;

    private double aliquotaEffettiva;
    private int    salaryScore;
    private String salaryBenchmark;

    // --- Getters & Setters ---

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { this.employeeName = v; }

    public int getAge() { return age; }
    public void setAge(int v) { this.age = v; }

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String v) { this.roleCategory = v; }

    public double getRalLorda() { return ralLorda; }
    public void setRalLorda(double v) { this.ralLorda = v; }

    public double getMensileLordo() { return mensileLordo; }
    public void setMensileLordo(double v) { this.mensileLordo = v; }

    public double getIrpefScaglione1() { return irpefScaglione1; }
    public void setIrpefScaglione1(double v) { this.irpefScaglione1 = v; }

    public double getIrpefScaglione2() { return irpefScaglione2; }
    public void setIrpefScaglione2(double v) { this.irpefScaglione2 = v; }

    public double getIrpefScaglione3() { return irpefScaglione3; }
    public void setIrpefScaglione3(double v) { this.irpefScaglione3 = v; }

    public double getIrpefTotaleAnnuo() { return irpefTotaleAnnuo; }
    public void setIrpefTotaleAnnuo(double v) { this.irpefTotaleAnnuo = v; }

    public double getIrpefMensile() { return irpefMensile; }
    public void setIrpefMensile(double v) { this.irpefMensile = v; }

    public double getDetrazioniLavoroDipendente() { return detrazioniLavoroDipendente; }
    public void setDetrazioniLavoroDipendente(double v) { this.detrazioniLavoroDipendente = v; }

    public double getInpsAnnuo() { return inpsAnnuo; }
    public void setInpsAnnuo(double v) { this.inpsAnnuo = v; }

    public double getInpsMensile() { return inpsMensile; }
    public void setInpsMensile(double v) { this.inpsMensile = v; }

    public double getNettoAnnuo() { return nettoAnnuo; }
    public void setNettoAnnuo(double v) { this.nettoAnnuo = v; }

    public double getNettoMensile() { return nettoMensile; }
    public void setNettoMensile(double v) { this.nettoMensile = v; }

    public double getAliquotaEffettiva() { return aliquotaEffettiva; }
    public void setAliquotaEffettiva(double v) { this.aliquotaEffettiva = v; }

    public int getSalaryScore() { return salaryScore; }
    public void setSalaryScore(int v) { this.salaryScore = v; }

    public String getSalaryBenchmark() { return salaryBenchmark; }
    public void setSalaryBenchmark(String v) { this.salaryBenchmark = v; }
}
