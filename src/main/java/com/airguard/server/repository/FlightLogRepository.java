package com.airguard.server.repository;

import com.airguard.server.entity.FlightLog;
import com.airguard.server.entity.Plane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FlightLogRepository extends JpaRepository<FlightLog, Integer> {
    // מוצא את לוג הטיסה הפתוח האחרון של מטוס ספציפי (לפני שנחת)
    Optional<FlightLog> findFirstByPlaneAndExitTimeIsNullOrderByEntryTimeDesc(Plane plane);
}