package com.airguard.server.model;

import java.util.List;

public class Runway {
    private String id;
    private List<String> allowedSizes;
    private double[] edge1; // קצה ראשון של המסלול [x, y]
    private double[] edge2; // קצה שני של המסלול [x, y]
    private boolean isOccupied;
    private String currentlyAssignedPlaneId; // שומר את ה-ID של המטוס שתפס אותו

    public Runway(String id, List<String> allowedSizes, double[] edge1, double[] edge2) {
        this.id = id;
        this.allowedSizes = allowedSizes;
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.isOccupied = false;
        this.currentlyAssignedPlaneId = null;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public List<String> getAllowedSizes() { return allowedSizes; }
    public double[] getEdge1() { return edge1; }
    public double[] getEdge2() { return edge2; }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    public String getCurrentlyAssignedPlaneId() { return currentlyAssignedPlaneId; }
    public void setCurrentlyAssignedPlaneId(String currentlyAssignedPlaneId) { this.currentlyAssignedPlaneId = currentlyAssignedPlaneId; }
}
