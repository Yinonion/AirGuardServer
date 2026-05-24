package com.airguard.server.controller;

import com.airguard.server.security.AesEncryptionService;
import com.airguard.server.service.AirSpaceService;
import com.airguard.server.service.TrafficGeneratorService;
import com.airguard.server.entity.Plane;
import org.springframework.web.bind.annotation.*; // includes CrossOrigin, GetMapping, RequestMapping, RestController
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.Map;


@RestController // אומר ל-Spring: זה הרכיב שמדבר עם האינטרנט
@RequestMapping("/api") // כל הכתובות יתחילו ב-/api
@CrossOrigin(origins = "*") // מרשה לדפדפנים ול-React לגשת למידע הזה (חשוב!)
public class AirGuardController {

    @Autowired
    private AirSpaceService airSpaceService; // חיבור ל"מוח" ששומר את המטוסים

    @Autowired
    private TrafficGeneratorService trafficGeneratorService;

    @Autowired
    private AesEncryptionService aesEncryptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // הכתובת תהיה: http://localhost:8080/api/planes
    @GetMapping("/planes")
    public Collection<Plane> getPlanes() {
        return airSpaceService.getAllPlanes();
    }

    /**
     * הוספת מטוס ידנית — מוגנת ב-AES.
     *
     * ה-Client צריך לשלוח JSON בפורמט:
     * { "payload": "<encrypted_base64_string>" }
     *
     * כאשר ה-encrypted_base64_string הוא תוצאת הצפנת ה-Plane object כ-JSON
     * עם timestamp, בעזרת אותו מפתח AES.
     *
     * תהליך:
     * 1. פענוח AES — כישלון = מקור לא מורשה → 403
     * 2. בדיקת timestamp — ישן מ-2 שניות → 403 (replay attack)
     * 3. קריאת ה-Plane מה-JSON המפוענח
     * 4. הוספה למערכת
     */
    @PostMapping("/planes")
    public ResponseEntity<?> addPlane(@RequestBody Map<String, String> encryptedRequest) {
        try {
            // 1. שליפת ה-payload המוצפן
            String encryptedPayload = encryptedRequest.get("payload");
            if (encryptedPayload == null || encryptedPayload.isBlank()) {
                return ResponseEntity.badRequest().body("Missing encrypted payload.");
            }

            // 2. פענוח + בדיקת timestamp
            String decryptedJson = aesEncryptionService.decryptAndValidate(encryptedPayload);

            // 3. המרה ל-Plane object
            Plane plane = objectMapper.readValue(decryptedJson, Plane.class);

            // 4. הוספה למערכת
            airSpaceService.addPlane(plane);
            System.out.println("✅ Secure manual plane added: " + plane.getId());
            return ResponseEntity.ok(plane);

        } catch (SecurityException e) {
            System.out.println("🚨 Security violation on /api/planes: " + e.getMessage());
            return ResponseEntity.status(403).body("Security check failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error processing plane request: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request format.");
        }
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