package com.airguard.server.controller;

import com.airguard.server.entity.User;
import com.airguard.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // מאפשר ל-React לגשת
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerController(@RequestBody User newUserRequest) {
        // 1. בדיקה אם תעודת הזהות כבר קיימת במערכת כדי למנוע כפילויות
        if (userRepository.findByIdNumber(newUserRequest.getIdNumber()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error: ID Number already exists in the system."));
        }

        // 2. יצירת המשתמש החדש (פקח)
        User newController = new User();
        newController.setIdNumber(newUserRequest.getIdNumber());
        newController.setFullName(newUserRequest.getFullName());

        // 3. הצפנת הסיסמה לפני השמירה בבסיס הנתונים (הכי חשוב!)
        newController.setPassword(passwordEncoder.encode(newUserRequest.getPassword()));

        // 4. כופה את התפקיד להיות 'Controller' (גם אם האקר ניסה לשלוח 'Admin' בבקשה)
        newController.setRole("Controller");

        // 5. שמירה ב-DB
        userRepository.save(newController);

        System.out.println("✅ New Controller Registered: " + newController.getFullName() + " | ID: " + newController.getIdNumber());

        return ResponseEntity.ok(Map.of("message", "Controller registered successfully!"));
    }
}