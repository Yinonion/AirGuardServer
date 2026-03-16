package com.airguard.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime; // לשימוש בתאריכים

@Entity
@Table(name = "FlightLog")
public class FlightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status; // Landed, Emergency...

    // קשר למטוס
    @ManyToOne
    @JoinColumn(name = "plane_id")
    private Plane plane;

    // קשר לפקח שניהל את האירוע
    @ManyToOne
    @JoinColumn(name = "controller_id")
    private User controller;

    // צור Getters ו-Setters


    public Integer getLogId() {
        return logId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public String getStatus() {
        return status;
    }

    public Plane getPlane() {
        return plane;
    }

    public User getController() {
        return controller;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPlane(Plane plane) {
        this.plane = plane;
    }

    public void setController(User controller) {
        this.controller = controller;
    }
}