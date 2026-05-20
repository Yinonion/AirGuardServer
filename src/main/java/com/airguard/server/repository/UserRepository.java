package com.airguard.server.repository;

import com.airguard.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // פונקציה מותאמת אישית: שליפת משתמש לפי תעודת זהות
    Optional<User> findByIdNumber(String idNumber);
}