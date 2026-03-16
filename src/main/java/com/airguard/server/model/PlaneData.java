package com.airguard.server.model;
import java.util.ArrayList;
import java.util.List;

public class PlaneData {
    // השמות כאן חייבים להיות זהים בדיוק לשמות במחלקת Plane במטוס!
    public String id;
    public double x;
    public double y;
    public double fuel = 100.0;
    public double heading;
    private double speed;
    private double altitude;
    private String size;
    private double weight;


    // נוסיף שדה סטטוס שיהיה לנו נוח בשרת
    public boolean isEmergency;

    // Getters
    public String getId() {return id;}

    public double getX() {return x;}

    public double getY() {return y;}

    public double getFuel() {return fuel;}

    public double getHeading() {return heading;}

    public boolean isEmergency() {return isEmergency;}

    public double getSpeed() {return speed;}

    public double getAltitude() {return altitude;}

    public String getSize() { return size; }

    public double getWeight() { return weight; }

    // Setters
    public void setId(String id) {this.id = id;}

    public void setX(double x) {this.x = x;}

    public void setY(double y) {this.y = y;}

    public void setFuel(double fuel) {this.fuel = fuel;}

    public void setHeading(double heading) {this.heading = heading;}

    public void setEmergency(boolean emergency) {isEmergency = emergency;}

    public void setSpeed(double speed) {this.speed = speed;}

    public void setAltitude(double altitude) {this.altitude = altitude;}

    public void setSize(String size) { this.size = size; }

    public void setWeight(double weight) { this.weight = weight; }

    // אופציונלי: הדפסה נוחה כדי שנראה בלוגים מה קורה
    @Override
    public String toString() {
        return "Plane " + id + " at (" + (int)x + "," + (int)y + ") Fuel: " + fuel + "%";
    }

    // רשימה שתשמור את היסטוריית המיקומים [Latitude, Longitude]
    private List<double[]> pathHistory = new ArrayList<>();

    public List<double[]> getPathHistory() {
        return pathHistory;
    }

}