package com.airguard.server.service;

import com.airguard.server.model.PlaneData;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;

@Service // אומר ל-Spring: "זה מנהל שירות, שמור עליו"
public class AirSpaceService {

    // הזיכרון החי: מפתח = מזהה מטוס, ערך = נתוני המטוס
    private final ConcurrentHashMap<String, PlaneData> activePlanes = new ConcurrentHashMap<>();

    // 1. עדכון מטוס (נקרא ע"י ה-Socket)
    public void updatePlane(PlaneData data) {
        activePlanes.put(data.id, data);
        // בהמשך נוסיף פה: בדיקת התנגשויות!
    }

    // 2. שליפת כל המטוסים (ייקרא ע"י האתר)
    public Collection<PlaneData> getAllPlanes() {
        return activePlanes.values();
    }

    // 3. הוספת מטוס חדש לרשימה (עבור המטוסים הידניים מהאתר)
    public void addPlane(PlaneData plane) {
        activePlanes.put(plane.getId(), plane);
        System.out.println("✈️ Plane added manually: " + plane.getId());
    }

    // 4. מחיקת מטוס (כשהוא נוחת/מתנתק/נמחק ע"י המשתמש)
    public void removePlane(String id) {
        activePlanes.remove(id);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
    public void updateManualPlanes() {
        for (PlaneData plane : activePlanes.values()) {
            if (plane.getId().startsWith("Man")) {

                // אם יש למטוס דלק, הוא טס וצורך דלק
                if (plane.getFuel() > 0) {
                    double speedKmh = plane.getSpeed();

                    // --- לוגיקת התנועה (נשאר אותו דבר) ---
                    double distanceKmPerSec = speedKmh / 3600.0;
                    double degreesPerSec = distanceKmPerSec / 111.0;
                    double radians = Math.toRadians(plane.getHeading());

                    plane.setX(plane.getX() + Math.cos(radians) * degreesPerSec);
                    plane.setY(plane.getY() + Math.sin(radians) * degreesPerSec);

                    // שומרים את המיקום הנוכחי (אחרי העדכון) בהיסטוריה בשביל השובל המקווקו
                    plane.getPathHistory().add(new double[]{plane.getX(), plane.getY()});

                    // כדי שהזיכרון של השרת לא יתפוצץ אחרי שעות של טיסה, נשמור רק את 50 המיקומים האחרונים
                    if (plane.getPathHistory().size() > 50) {
                        plane.getPathHistory().remove(0);
                    }

                    // --- אלגוריתם צריכת הדלק החדש ---
                    // נוסחה: (מהירות / 500) * (משקל / 100) * פקטור בסיס
                    // ככל שהמשקל או המהירות גבוהים יותר, המכפלה גדלה והדלק יורד מהר יותר
                    double weightFactor = plane.getWeight() / 100.0; // מטוס של 200 טון ייתן פקטור 2
                    double speedFactor = speedKmh / 500.0;
                    double fuelBurn = speedFactor * weightFactor * 0.2; // 0.2 זה קצב שריפה בסיסי לשנייה בסימולציה

                    // מוריד את הדלק, ומוודא שלא יורד מתחת ל-0
                    plane.setFuel(Math.max(0, plane.getFuel() - fuelBurn));
                } else {
                    // נגמר הדלק! המטוס נעצר.
                    plane.setSpeed(0);
                }
            }
        }
    }
}