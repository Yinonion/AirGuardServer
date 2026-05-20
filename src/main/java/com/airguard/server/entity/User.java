package com.airguard.server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(unique = true, nullable = false)
    private String idNumber;

    private String fullName;
    private String password;
    private String role; // "Admin" or "Controller"

    // בנאי ריק חובה עבור JPA
    public User() {}

    public Integer getUserId() { return userId; }
    public String getIdNumber() { return idNumber; }
    public String getFullName() { return fullName; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    public void setUserId(Integer userId) { this.userId = userId; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}