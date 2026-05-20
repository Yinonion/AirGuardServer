package com.airguard.server.service; // ודא שזה תואם לשם החבילה שלך

import com.airguard.server.entity.Plane;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

@Service
public class PhysicsEngineService {

    private final AirSpaceService airSpaceService;

    @Autowired
    public PhysicsEngineService(AirSpaceService airSpaceService) {
        this.airSpaceService = airSpaceService;
    }

    @Scheduled(fixedRate = 1000)
    public void applyPhysicsToAllPlanes() {
        Collection<Plane> activePlanes = airSpaceService.getAllPlanes();

        for (Plane plane : activePlanes) {

            // מזיזים את המטוס רק אם יש לו דלק, ורק אם הוא באמת טס (מהירות גדולה מ-0)
            if (plane.getFuel() > 0 && plane.getSpeed() > 0) {

                // --- אלגוריתם פנייה הדרגתית (הקוד המקורי שלך!) ---
                double currentHeading = plane.getHeading();
                double target = plane.getTargetHeading();

                if (currentHeading != target) {
                    double diff = (target - currentHeading + 540) % 360 - 180;
                    double turnRate = 4.0; // קצב הפנייה המקורי שלך: 4 מעלות לשנייה

                    if (Math.abs(diff) <= turnRate) {
                        plane.setHeading(target); // התיישרנו!
                    } else {
                        plane.setHeading((currentHeading + Math.signum(diff) * turnRate + 360) % 360);
                    }
                }

                // --- תנועה מדויקת לפי קנה מידה גיאוגרפי (הנוסחה המעולה שלך!) ---
                double speedKmh = plane.getSpeed();
                double distanceKmPerSec = speedKmh / 3600.0;
                double degreesPerSec = distanceKmPerSec / 111.0;
                double radians = Math.toRadians(plane.getHeading());

                plane.setX(plane.getX() + Math.cos(radians) * degreesPerSec);
                plane.setY(plane.getY() + Math.sin(radians) * degreesPerSec);

                // --- שמירת השובל המקווקו (שכחתי את זה קודם!) ---
                if (plane.getPathHistory() != null) {
                    plane.getPathHistory().add(new double[]{plane.getX(), plane.getY()});
                    if (plane.getPathHistory().size() > 50) {
                        plane.getPathHistory().remove(0);
                    }
                }

                // --- אלגוריתם צריכת הדלק המקורי והחכם שלך ---
                double weightFactor = Math.max(0.1, plane.getWeight() / 100.0);
                double speedFactor = speedKmh / 500.0;
                double fuelBurn = speedFactor * weightFactor * 0.015;

                plane.setFuel(Math.max(0, plane.getFuel() - fuelBurn));

            } else if (plane.getFuel() <= 0) {
                // נגמר הדלק - המטוס נופל/נעצר
                plane.setSpeed(0);
            }
        }
    }
}