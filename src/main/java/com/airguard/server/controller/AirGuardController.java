package com.airguard.server.controller;

import com.airguard.server.service.AirSpaceService;
import com.airguard.server.service.TrafficGeneratorService;
import com.airguard.server.entity.Plane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController // אומר ל-Spring: זה הרכיב שמדבר עם האינטרנט
@RequestMapping("/api") // כל הכתובות יתחילו ב-/api
@CrossOrigin(origins = "*") // מרשה לדפדפנים ול-React לגשת למידע הזה (חשוב!)
public class AirGuardController {

    @Autowired
    private AirSpaceService airSpaceService; // חיבור ל"מוח" ששומר את המטוסים

    @Autowired
    private TrafficGeneratorService trafficGeneratorService;

    // הכתובת תהיה: http://localhost:8080/api/planes
    @GetMapping("/planes")
    public Collection<Plane> getPlanes() {
        return airSpaceService.getAllPlanes();
    }

    // 1. הוספת מטוס ידנית (מהאתר)
    @PostMapping("/planes")
    public Plane addPlane(@RequestBody Plane plane) {
        airSpaceService.addPlane(plane);
        System.out.println("➕ Manual plane added: " + plane.getId());
        return plane;
    }

    // 2. מחיקת מטוס (מהאתר)
    @DeleteMapping("/planes/{id}")
    public void deletePlane(@PathVariable String id) {
        airSpaceService.removePlane(id);
        System.out.println("🗑️ Manual plane deleted: " + id);
    }

    @PostMapping("/toggle-spawn") // או שתוסיף /api/ לפני אם ככה עשית בשאר הפונקציות
    public boolean toggleAutoSpawn() {
        // בודק מה המצב הנוכחי, והופך אותו (אם דלוק מכבה, אם מכובה מדליק)
        boolean currentState = trafficGeneratorService.isAutoSpawnEnabled();
        trafficGeneratorService.setAutoSpawnEnabled(!currentState);

        // מחזיר ל-React את המצב החדש כדי שיוכל לעדכן את הצבע של הכפתור
        return !currentState;
    }
}