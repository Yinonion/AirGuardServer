package com.airguard.server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Planes")
public class Plane {

    @Id
    @Column(length = 20)
    private String planeId; // מספר זנב (למשל: 4X-EBA). שים לב - אין @GeneratedValue!

    private String model;

    private String sizeCategory; // Small, Medium, Large

    // --- הקשר לחברת התעופה ---
    @ManyToOne // הרבה מטוסים שייכים לחברה אחת
    @JoinColumn(name = "airline_id") // זה שם העמודה ב-SQL שתקשר ביניהם
    private Airline airline;

    // צור Getters ו-Setters

    public String getPlaneId() {
        return planeId;
    }

    public String getModel() {
        return model;
    }

    public String getSizeCategory() {
        return sizeCategory;
    }

    public Airline getAirline() {
        return airline;
    }

    public void setPlaneId(String planeId) {
        this.planeId = planeId;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setSizeCategory(String sizeCategory) {
        this.sizeCategory = sizeCategory;
    }

    public void setAirline(Airline airline) {
        this.airline = airline;
    }
}