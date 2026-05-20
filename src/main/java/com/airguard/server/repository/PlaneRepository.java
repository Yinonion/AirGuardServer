package com.airguard.server.repository;

import com.airguard.server.entity.Plane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaneRepository extends JpaRepository<Plane, String> {
    // String מייצג את סוג הנתונים של מספר הזנב (planeId)
}