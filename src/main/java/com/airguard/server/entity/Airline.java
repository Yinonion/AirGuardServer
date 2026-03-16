package com.airguard.server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Airlines")
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // זה אומר ל-SQL: תמספר לבד (1, 2, 3...)
    private Integer airlineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    // --- חייבים Getters ו-Setters לכל השדות ---
    // טיפ של מקצוענים: אל תכתוב אותם ידנית!
    // לחץ בתוך המחלקה: Alt + Insert -> בחר Getter and Setter -> סמן את הכל ואישור.


    public Integer getAirlineId() {
        return airlineId;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public void setAirlineId(Integer airlineId) {
        this.airlineId = airlineId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}