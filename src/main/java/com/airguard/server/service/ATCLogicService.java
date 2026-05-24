package com.airguard.server.service;

import com.airguard.server.model.FlightQueue;
import com.airguard.server.model.Runway;
import com.airguard.server.entity.Plane;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ATCLogicService {

    private final AirSpaceService airSpaceService;

    private FlightQueue smallPlanesQueue = new FlightQueue();
    private FlightQueue mediumPlanesQueue = new FlightQueue();
    private FlightQueue largePlanesQueue = new FlightQueue();

    private final double FAF = 0.08; // Final Approach Fix | 0.08 degrees is about 8.5 Km

    // *** קבוע מרוכז למרחק נקודת ה-IF (יעד ניווט כשהמטוס עדיין לא מיושר) מסף המסלול ***
    // שימוש גם בשידוך מסלולים וגם בניווט
    private final double IF_DISTANCE = 0.15;

    @Autowired
    public ATCLogicService(AirSpaceService airSpaceService) {
        this.airSpaceService = airSpaceService;
    }

    private double calculateHeadingToTarget(double fromX, double fromY, double toX, double toY) {
        double angleDeg = Math.toDegrees(Math.atan2(toY - fromY, toX - fromX));
        if (angleDeg < 0) angleDeg += 360;
        return angleDeg;
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    // *** מחשב את נקודת ה-IF (Initial Fix) עבור קצה מסלול נתון ***
    // ה-IF נמצא במרחק IF_DISTANCE (0.15 מעלות שזה 16.5 ק"מ) מהקצה, בכיוון הנגדי לכיוון המסלול
    private double[] calculateIF(double[] threshold, double[] otherEdge) {
        double runwayHeading = calculateHeadingToTarget(threshold[0], threshold[1], otherEdge[0], otherEdge[1]);
        double backHeading = (runwayHeading + 180) % 360;
        double ifX = threshold[0] + Math.cos(Math.toRadians(backHeading)) * IF_DISTANCE;
        double ifY = threshold[1] + Math.sin(Math.toRadians(backHeading)) * IF_DISTANCE;
        return new double[]{ifX, ifY};
    }

    // *** בוחר את קצה המסלול הנכון לנחיתה לפי קרבה ל-IF (לא לקצה עצמו!) ***
    // כך המטוס תמיד מגיע מהכיוון הנכון של ציר הגישה
    private double[] selectCorrectThreshold(Plane plane, Runway rwy) {
        double[] if1 = calculateIF(rwy.getEdge1(), rwy.getEdge2());
        double[] if2 = calculateIF(rwy.getEdge2(), rwy.getEdge1());
        double distToIF1 = calculateDistance(plane.getX(), plane.getY(), if1[0], if1[1]);
        double distToIF2 = calculateDistance(plane.getX(), plane.getY(), if2[0], if2[1]);
        // בוחרים את הקצה שה-IF שלו קרוב יותר למטוס
        return (distToIF1 < distToIF2) ? rwy.getEdge1() : rwy.getEdge2();
    }

    // אלגוריתם שידוך מסלולים עם מנגנון גניבה חכם לפי מרחק
    private double[] assignRunway(Plane plane) {
        List<Runway> runways = airSpaceService.getRunways();
        Map<String, Plane> activePlanes = airSpaceService.getActivePlanesMap();

        Runway bestRunway = null;
        double[] targetEdge = null;
        double shortestDistance = Double.MAX_VALUE;

        // שלב 1: חיפוש מסלול פנוי לחלוטין
        for (Runway rwy : runways) {
            if (rwy.getAllowedSizes().contains(plane.getSize()) && !rwy.isOccupied()) {
                // *** בחירת קצה לפי קרבה ל-IF, לא לקצה עצמו ***
                double[] candidateThreshold = selectCorrectThreshold(plane, rwy);
                double[] otherEdge = (candidateThreshold == rwy.getEdge1()) ? rwy.getEdge2() : rwy.getEdge1();
                double[] candidateIF = calculateIF(candidateThreshold, otherEdge);
                double distToIF = calculateDistance(plane.getX(), plane.getY(), candidateIF[0], candidateIF[1]);

                if (distToIF < shortestDistance) {
                    shortestDistance = distToIF;
                    bestRunway = rwy;
                    targetEdge = candidateThreshold;
                }
            }
        }

        // שלב 1ב: אופטימיזציית מרחקים - גניבת מסלול (Relative Distance Preemption)
        if (bestRunway == null) {
            for (Runway rwy : runways) {
                if (rwy.getAllowedSizes().contains(plane.getSize()) && rwy.isOccupied()) {
                    Plane occupyingPlane = activePlanes.get(rwy.getCurrentlyAssignedPlaneId());
                    if (occupyingPlane != null && occupyingPlane.getState().equals("APPROACH")) {
                        double occupyingDist = occupyingPlane.getDistanceToAirport();
                        double myDist = plane.getDistanceToAirport();

                        // התיקון: אם אני קרוב יותר מהמטוס השני ביחסית הרבה (0.15), והוא עדיין רחוק מספיק (0.35) - אני לוקח!
                        if (occupyingDist - myDist > 0.15 && occupyingDist > 0.35) {
                            System.out.println("🔄 ATC Optimization: " + plane.getId() + " stole runway from " + occupyingPlane.getId());
                            occupyingPlane.setState("EN_ROUTE");
                            occupyingPlane.setAssignedRunway(null); // מוחקים לו את השיוך

                            bestRunway = rwy;
                            // *** גם כאן בחירת קצה לפי IF ***
                            targetEdge = selectCorrectThreshold(plane, rwy);
                            break;
                        }
                    }
                }
            }
        }

        if (bestRunway != null) {
            bestRunway.setOccupied(true);
            bestRunway.setCurrentlyAssignedPlaneId(plane.getId());
            plane.setState("APPROACH");
            plane.setAssignedRunway(bestRunway.getId());
            return targetEdge;
        }

        // שלב 2: הפקעת מסלול בחירום
        if (plane.isEmergency()) {
            for (Runway rwy : runways) {
                if (rwy.getAllowedSizes().contains(plane.getSize()) && rwy.isOccupied()) {
                    Plane occupyingPlane = activePlanes.get(rwy.getCurrentlyAssignedPlaneId());
                    if (occupyingPlane != null && occupyingPlane.getState().equals("APPROACH")) {
                        double distToAirport = occupyingPlane.getDistanceToAirport();
                        // תיקון: מטוס בחירום לא יפקיע מסלול ממטוס שכבר נמצא בפיינל הסופי (קרוב מ-0.07)
                        if (distToAirport > 0.07) {
                            System.out.println("🚨 ATC EMERGENCY PREEMPTION! " + plane.getId() + " is taking over runway from " + occupyingPlane.getId() + " 🚨");
                            occupyingPlane.setState("EN_ROUTE"); // המפונה ממשיך לטוס רגיל
                            occupyingPlane.setAssignedRunway(null); // מוחקים למטוס המפונה את השיוך למסלול ב-Java
                            double dist1 = calculateDistance(plane.getX(), plane.getY(), rwy.getEdge1()[0], rwy.getEdge1()[1]);
                            double dist2 = calculateDistance(plane.getX(), plane.getY(), rwy.getEdge2()[0], rwy.getEdge2()[1]);
                            targetEdge = (dist1 < dist2) ? rwy.getEdge1() : rwy.getEdge2();
                            rwy.setCurrentlyAssignedPlaneId(plane.getId()); // מוודאים שגם למטוס החירום מעודכן שם המסלול
                            plane.setState("APPROACH");
                            return targetEdge;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Scheduled(fixedRate = 1000)
    public void runATCEngine() {
        List<Runway> runways = airSpaceService.getRunways();
        Map<String, Plane> activePlanes = airSpaceService.getActivePlanesMap();

        // 1. ניקוי מסלולים ואיפוס שיוכים למטוסים שנחתו
        for (Runway rwy : runways) {
            if (rwy.isOccupied()) {
                Plane p = activePlanes.get(rwy.getCurrentlyAssignedPlaneId());
                if (p == null || p.getState().equals("LANDED")) {
                    rwy.setOccupied(false);
                    rwy.setCurrentlyAssignedPlaneId(null);
                    if (p != null) p.setAssignedRunway(null);
                }
            }
        }

        // הפעלת סורק מניעת ההתנגשויות בכל שנייה
        checkAndResolveCollisions(activePlanes);

        // 2. איפוס תורים
        smallPlanesQueue = new FlightQueue();
        mediumPlanesQueue = new FlightQueue();
        largePlanesQueue = new FlightQueue();

        double ATC_RADAR_RANGE = 1.5;

        // 3. איסוף מטוסים וניהול מצבי טיסה (EN_ROUTE -> HOLDING)
        for (Plane plane : activePlanes.values()) {
            if (plane.getAltitude() > 0 && plane.getDistanceToAirport() <= ATC_RADAR_RANGE) {

                if (plane.getState().equals("CRUISE")) {
                    plane.setState("EN_ROUTE");
                }

                if (plane.getFuel() < 20.0 && !plane.isEmergency()) {
                    System.out.println("🚨 BINGO FUEL EMERGENCY: " + plane.getId());
                    plane.setEmergency(true);
                }

                if (plane.getState().equals("EN_ROUTE") && plane.getDistanceToAirport() <= 0.2) {
                    // התיקון: רק אם זה לא מטוס בחירום, תכניס אותו להמתנה באוויר
                    if (!plane.isEmergency()) {
                        plane.setState("HOLDING");
                    }
                }

                if (plane.getState().equals("HOLDING") || plane.getState().equals("EN_ROUTE")) {
                    if (plane.getSize().equals("Small")) smallPlanesQueue.insert(plane);
                    else if (plane.getSize().equals("Medium")) mediumPlanesQueue.insert(plane);
                    else if (plane.getSize().equals("Large")) largePlanesQueue.insert(plane);
                }
            }
        }

        // 4. שידוך מסלולים למטוסים הממתינים
        Plane topSmall = smallPlanesQueue.pollHighestPriority();
        if (topSmall != null) assignRunway(topSmall);
        Plane topMedium = mediumPlanesQueue.pollHighestPriority();
        if (topMedium != null) assignRunway(topMedium);
        Plane topLarge = largePlanesQueue.pollHighestPriority();
        if (topLarge != null) assignRunway(topLarge);

        // ניהול גבהים במגדל ההמתנה האנכי
        List<Plane> holdingPlanes = activePlanes.values().stream()
                .filter(p -> p.getState().equals("HOLDING"))
                .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .collect(Collectors.toList());

        for (int i = 0; i < holdingPlanes.size(); i++) {
            Plane p = holdingPlanes.get(i);
            double targetHoldingAltitude = 7000.0 + (i * 1000.0);
            if (p.getAltitude() < targetHoldingAltitude) {
                p.setAltitude(Math.min(targetHoldingAltitude, p.getAltitude() + 300));
            } else if (p.getAltitude() > targetHoldingAltitude) {
                p.setAltitude(Math.max(targetHoldingAltitude, p.getAltitude() - 300));
            }
        }

        // 5. ניווט והכוונה בזמן אמת
        for (Plane plane : activePlanes.values()) {
            if (plane.getDistanceToAirport() <= ATC_RADAR_RANGE) {

                if (plane.getState().equals("EN_ROUTE")) {
                    double targetHeading = calculateHeadingToTarget(plane.getX(), plane.getY(), 32.01, 34.88);
                    plane.setTargetHeading(targetHeading);
                }
                else if (plane.getState().equals("APPROACH") || plane.getState().equals("FINAL_APPROACH")) {
                    Runway myRwy = null;
                    for (Runway rwy : runways) {
                        if (plane.getId().equals(rwy.getCurrentlyAssignedPlaneId())) {
                            myRwy = rwy; break;
                        }
                    }

                    if (myRwy != null) {
                        // מנגנון הבראה עצמית: כופה ומסנכרן את שדה המסלול בכל שנייה מחדש!
                        plane.setAssignedRunway(myRwy.getId());

                        // תיקון ה-Typo שהיה כאן (myRwy במקום myRwy)
                        double dist1 = calculateDistance(plane.getX(), plane.getY(), myRwy.getEdge1()[0], myRwy.getEdge1()[1]);
                        double dist2 = calculateDistance(plane.getX(), plane.getY(), myRwy.getEdge2()[0], myRwy.getEdge2()[1]);
                        double[] targetEdge = (dist1 < dist2) ? myRwy.getEdge1() : myRwy.getEdge2();
                        double[] otherEdge = (dist1 < dist2) ? myRwy.getEdge2() : myRwy.getEdge1();

                        double distanceToThreshold = Math.min(dist1, dist2);

                        double runwayHeading = calculateHeadingToTarget(targetEdge[0], targetEdge[1], otherEdge[0], otherEdge[1]);
                        double backHeading = (runwayHeading + 180) % 360;

                        double fafX = targetEdge[0] + Math.cos(Math.toRadians(backHeading)) * 0.08;
                        double fafY = targetEdge[1] + Math.sin(Math.toRadians(backHeading)) * 0.08;
                        double ifX = targetEdge[0] + Math.cos(Math.toRadians(backHeading)) * IF_DISTANCE;
                        double ifY = targetEdge[1] + Math.sin(Math.toRadians(backHeading)) * IF_DISTANCE;

                        double currentAngleToRunway = calculateHeadingToTarget(plane.getX(), plane.getY(), targetEdge[0], targetEdge[1]);
                        double diffAngle = Math.abs((currentAngleToRunway - runwayHeading + 540) % 360 - 180);

                        // פרופיל מהירות וגובה הדרגתי
                        if (distanceToThreshold > 0.18) {
                            if (plane.getSpeed() > 500) plane.setSpeed(Math.max(500, plane.getSpeed() - 5));
                            if (plane.getAltitude() > 10000) plane.setAltitude(Math.max(10000, plane.getAltitude() - 300));
                        } else if (distanceToThreshold > 0.08) {
                            if (plane.getSpeed() > 250) plane.setSpeed(Math.max(250, plane.getSpeed() - 8));
                            if (plane.getAltitude() > 3000) plane.setAltitude(Math.max(3000, plane.getAltitude() - 300));
                        } else {
                            // מעבר ל-FINAL_APPROACH רק אם המטוס כבר מיושר עם ציר המסלול (diffAngle קטן)
                            // וגם כבר ירד לגובה סביר (מתחת ל-4000 רגל) — כלומר הוא לא זה עתה יצא מ-HOLDING
                            // מקבל חסינות מ-TCAS
                            if (diffAngle <= 15 && plane.getAltitude() < 4000) {
                                plane.setState("FINAL_APPROACH");
                            }

                            if (plane.getSpeed() > 250) plane.setSpeed(Math.max(250, plane.getSpeed() - 10));
                            double desiredAltitude = (distanceToThreshold / 0.08) * 3000;
                            if (plane.getAltitude() > desiredAltitude) plane.setAltitude(Math.max(0, plane.getAltitude() - 200));
                        }

                        // ניווט ויישור ציר מסלול
                        if (distanceToThreshold < 0.005) {
                            plane.setState("LANDING");
                            plane.setAltitude(0);
                            plane.setX(targetEdge[0]);
                            plane.setY(targetEdge[1]);
                            plane.setHeading(runwayHeading);
                            plane.setTargetHeading(runwayHeading);
                        } else if (diffAngle > 15) {
                            plane.setTargetHeading(calculateHeadingToTarget(plane.getX(), plane.getY(), ifX, ifY));
                        } else if (distanceToThreshold > 0.08) {
                            plane.setTargetHeading(calculateHeadingToTarget(plane.getX(), plane.getY(), fafX, fafY));
                        } else {
                            plane.setTargetHeading(calculateHeadingToTarget(plane.getX(), plane.getY(), targetEdge[0], targetEdge[1]));
                        }
                    }
                }
                else if (plane.getState().equals("LANDING")) {
                    // המטוס ממשיך להחזיק במסלול המוקצה גם בזמן הבלימה על הקרקע
                    plane.setSpeed(Math.max(0, plane.getSpeed() - 10));
                    if (plane.getSpeed() == 0) {
                        plane.setState("LANDED");
                        plane.setAssignedRunway(null);
                        airSpaceService.removePlane(plane.getId());
                    }
                }
                else if (plane.getState().equals("HOLDING")) {
                    double pX = plane.getX();
                    double pY = plane.getY();
                    double targetX, targetY;

                    double northLat = 32.06;
                    double southLat = 31.96;
                    double eastLon = 35.01;
                    double westLon = 34.75;

                    if (pX >= northLat && pY < eastLon) {
                        targetX = northLat; targetY = eastLon;
                    } else if (pY >= eastLon && pX > southLat) {
                        targetX = southLat; targetY = eastLon;
                    } else if (pX <= southLat && pY > westLon) {
                        targetX = southLat; targetY = westLon;
                    } else {
                        targetX = northLat; targetY = westLon;
                    }
                    plane.setTargetHeading(calculateHeadingToTarget(pX, pY, targetX, targetY));
                }
            }
        }
    }

    // אלגוריתם TCAS - סריקה ומניעת התנגשויות בזמן אמת
    private void checkAndResolveCollisions(Map<String, Plane> activePlanes) {
        List<Plane> planesList = new ArrayList<>(activePlanes.values());

        // לולאה כפולה להשוואה בין כל זוג מטוסים במרחב
        for (int i = 0; i < planesList.size(); i++) {
            for (int j = i + 1; j < planesList.size(); j++) {
                Plane p1 = planesList.get(i);
                Plane p2 = planesList.get(j);

                // בודקים רק מטוסים שנמצאים באוויר (גובה מעל 0)
                if (p1.getAltitude() > 0 && p2.getAltitude() > 0) {

                    // 1. חישוב מרחק אופקי (ק"מ/מעלות)
                    double horizontalDist = calculateDistance(p1.getX(), p1.getY(), p2.getX(), p2.getY());

                    // 2. חישוב הפרש גבהים אנכי
                    double verticalDist = Math.abs(p1.getAltitude() - p2.getAltitude());

                    // הגדרת רדיוס סכנה: פחות מ-0.05 מרחק אופקי ופחות מ-1000 רגל הפרש גבהים
                    if (horizontalDist < 0.05 && verticalDist < 1000) {

                        boolean p1Emerg = p1.isEmergency();
                        boolean p2Emerg = p2.isEmergency();
                        boolean p1Final = p1.getState().equals("FINAL_APPROACH");
                        boolean p2Final = p2.getState().equals("FINAL_APPROACH");

                        // 1. שני המטוסים בפיינל מקביל, או אחד בפיינל ואחד בחירום - מתעלמים לחלוטין!
                        if ((p1Final && p2Final) || (p1Emerg && p2Final) || (p2Emerg && p1Final)) {
                            System.out.println("✈️ TCAS Muted: " + p1.getId() + " and " + p2.getId() + " are established on safe parallel finals.");
                            continue; // מדלגים על פקודת ההתחמקות
                        }

                        // 2. שניהם בחירום! הקרוב נוחת, הרחוק מטפס מעט וחוזר לנחיתה
                        else if (p1Emerg && p2Emerg) {
                            Plane aborting = (p1.getDistanceToAirport() > p2.getDistanceToAirport()) ? p1 : p2;
                            aborting.setAltitude(aborting.getAltitude() + 400); // עולה קצת לתת ספייס
                            // אנחנו לא משנים לו כיוון, שיישאר על הציר ופשוט יעכב את הירידה שלו
                        }

                        // 3. אחד בחירום, השני במצב רגיל/Approach (הרגיל בורח)
                        else if (p1Emerg && !p2Emerg) {
                            p2.setAltitude(p2.getAltitude() + 600);
                            p2.setTargetHeading((p2.getTargetHeading() + 45) % 360);
                        }

                        else if (p2Emerg && !p1Emerg) {
                            p1.setAltitude(p1.getAltitude() + 600);
                            p1.setTargetHeading((p1.getTargetHeading() + 45) % 360);
                        }

                        // 4. אחד בפיינל, השני במצב רגיל/Approach/Holding (הרגיל בורח)
                        else if (p1Final && !p2Final) {
                            p2.setAltitude(p2.getAltitude() + 600);
                            p2.setTargetHeading((p2.getTargetHeading() + 45) % 360);
                        }

                        else if (p2Final && !p1Final) {
                            p1.setAltitude(p1.getAltitude() + 600);
                            p1.setTargetHeading((p1.getTargetHeading() + 45) % 360);
                        }

                        // 5. TCAS סטנדרטי למטוסים רגילים (אף אחד לא בפיינל ולא בחירום)
                        else {
                            if (p1.getAltitude() >= p2.getAltitude()) {
                                p1.setAltitude(p1.getAltitude() + 400);
                                p2.setAltitude(Math.max(1000, p2.getAltitude() - 400));
                                p1.setTargetHeading((p1.getTargetHeading() + 20) % 360);
                                p2.setTargetHeading((p2.getTargetHeading() - 20 + 360) % 360);
                            } else {
                                p2.setAltitude(p2.getAltitude() + 400);
                                p1.setAltitude(Math.max(1000, p1.getAltitude() - 400));
                                p2.setTargetHeading((p2.getTargetHeading() + 20) % 360);
                                p1.setTargetHeading((p1.getTargetHeading() - 20 + 360) % 360);
                            }
                        }
                    }
                }
            }
        }
    }
}