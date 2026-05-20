package com.airguard.server.config;

import com.airguard.server.entity.Airline;
import com.airguard.server.entity.User;
import com.airguard.server.repository.AirlineRepository;
import com.airguard.server.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // אומר ל-Spring Boot לנהל את הקלאס הזה ולהריץ אותו אוטומטית בעלייה
public class DataInitializer implements CommandLineRunner {

    // הזרקת ה-Repositories באמצעות Constructor Injection (התקן המקצועי ביותר)
    private final AirlineRepository airlineRepository;
    private final UserRepository userRepository;

    public DataInitializer(AirlineRepository airlineRepository, UserRepository userRepository) {
        this.airlineRepository = airlineRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. הכנסת חברות תעופה ראשוניות (רק אם הטבלה ריקה לגמרי)
        if (airlineRepository.count() == 0) {
            Airline elal = new Airline();
            elal.setName("El Al");
            elal.setCountry("Israel");
            airlineRepository.save(elal);

            Airline united = new Airline();
            united.setName("United Airlines");
            united.setCountry("USA");
            airlineRepository.save(united);

            Airline lufthansa = new Airline();
            lufthansa.setName("Lufthansa");
            lufthansa.setCountry("Germany");
            airlineRepository.save(lufthansa);

            System.out.println("🌱 [SQL Seed]: Sample airlines inserted successfully!");
        }

        // 2. הכנסת משתמשים (פקחים ומנהל) ראשוניים (רק אם הטבלה ריקה לגמרי)
        if (userRepository.count() == 0) {
            // יצירת מנהל מערכת
            User admin = new User();
            admin.setIdNumber("123456789");
            admin.setFullName("מנהל מערכת");
            admin.setPassword("admin123"); // זמני, בהמשך הדרך נצפין אותה עם BCrypt עבור ה-JWT
            admin.setRole("Admin");
            userRepository.save(admin);

            // יצירת פקח טיסה
            User controller = new User();
            controller.setIdNumber("987654321");
            controller.setFullName("פקח טיסה אלפא");
            controller.setPassword("user123");
            controller.setRole("Controller");
            userRepository.save(controller);

            System.out.println("🌱 [SQL Seed]: Sample users inserted successfully!");
        }
    }
}