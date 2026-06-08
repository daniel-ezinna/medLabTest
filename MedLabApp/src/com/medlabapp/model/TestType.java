package com.medlabapp.model;

public class TestType {
    private int id;
    private String name;
    private double price;
    private int tatHours; // Turnaround Time
    private String resultFormat; // NUMERIC, TEXT, PDF, IMAGE

    public TestType(int id, String name, double price, int tatHours, String resultFormat) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.tatHours = tatHours;
        this.resultFormat = resultFormat;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getTatHours() { return tatHours; }
    public String getResultFormat() { return resultFormat; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setTatHours(int tatHours) { this.tatHours = tatHours; }
    public void setResultFormat(String resultFormat) { this.resultFormat = resultFormat; }
    
    // Override toString so JavaFX ComboBoxes display the name nicely instead of a memory address
    @Override
    public String toString() {
        return name + " (₦" + price + ")";
    }
}