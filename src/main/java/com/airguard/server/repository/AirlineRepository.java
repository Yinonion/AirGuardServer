package com.airguard.server.repository;

import com.airguard.server.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Integer> {
    // Integer מייצג את סוג הנתונים של המפתח הראשי (airlineId)
}