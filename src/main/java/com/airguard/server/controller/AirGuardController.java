package com.airguard.server.controller;

import com.airguard.server.model.PlaneData;
import com.airguard.server.service.AirSpaceService;
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

    // הכתובת תהיה: http://localhost:8080/api/planes
    @GetMapping("/planes")
    public Collection<PlaneData> getPlanes() {
        return airSpaceService.getAllPlanes();
    }

    // 1. הוספת מטוס ידנית (מהאתר)
    @PostMapping("/planes")
    public PlaneData addPlane(@RequestBody PlaneData plane) {
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
}