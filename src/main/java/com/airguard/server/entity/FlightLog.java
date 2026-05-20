package com.airguard.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FlightLog")
public class FlightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private String status; // Landed, Emergency...

    @ManyToOne
    @JoinColumn(name = "plane_id")
    private Plane plane;

    @ManyToOne
    @JoinColumn(name = "controller_id")
    private User controller;

    // בנאי ריק חובה עבור JPA
    public FlightLog() {}

    public Integer getLogId() { return logId; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public String getStatus() { return status; }
    public Plane getPlane() { return plane; }
    public User getController() { return controller; }

    public void setLogId(Integer logId) { this.logId = logId; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    public void setStatus(String status) { this.status = status; }
    public void setPlane(Plane plane) { this.plane = plane; }
    public void setController(User controller) { this.controller = controller; }
}