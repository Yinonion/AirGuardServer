package com.airguard.server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Airlines")
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer airlineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    // בנאי ריק חובה עבור JPA
    public Airline() {}

    public Integer getAirlineId() { return airlineId; }
    public String getName() { return name; }
    public String getCountry() { return country; }

    public void setAirlineId(Integer airlineId) { this.airlineId = airlineId; }
    public void setName(String name) { this.name = name; }
    public void setCountry(String country) { this.country = country; }
}