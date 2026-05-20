package com.airguard.server.config;

import com.airguard.server.entity.Airline;
import com.airguard.server.repository.AirlineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // אומר ל-Spring Boot לנהל את הקלאס הזה ולהריץ אותו אוטומטית בעלייה
public class DataInitializer implements CommandLineRunner {

    // הזרקת ה-Repositories באמצעות Constructor Injection (התקן המקצועי ביותר)
    @Autowired
    private final AirlineRepository airlineRepository;

    public DataInitializer(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // הכנסת חברות תעופה ראשוניות (רק אם הטבלה ריקה לגמרי)
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
    }
}