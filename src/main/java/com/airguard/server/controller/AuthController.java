package com.airguard.server.controller;

import com.airguard.server.entity.User;
import com.airguard.server.repository.UserRepository;
import com.airguard.server.security.CustomUserDetailsService;
import com.airguard.server.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // פונקציה שתייצר לנו משתמש Admin התחלתי כדי שנוכל להיכנס למערכת!
    @jakarta.annotation.PostConstruct
    public void initDefaultAdmin() {
        if (userRepository.findByIdNumber("329329734").isEmpty()) {
            User admin = new User();
            admin.setIdNumber("329329734");
            admin.setFullName("Yinon Levi");
            admin.setPassword(passwordEncoder.encode("AdminFreddie")); // כאן אנחנו מצפינים את הסיסמה ל-DB!
            admin.setRole("Admin");
            userRepository.save(admin);
            System.out.println("✅ Default Admin User Created! ID: 329329734 | Password: AdminFreddie");
        }
    }

    // --- מחלקות פנימיות לייצוג המידע שעובר מה-React ל-Java ולהפך ---

    public static class AuthRequest {
        private String idNumber;
        private String password;

        public String getIdNumber() { return idNumber; }
        public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private String role;
        private String fullName;

        public AuthResponse(String token, String role, String fullName) {
            this.token = token;
            this.role = role;
            this.fullName = fullName;
        }

        public String getToken() { return token; }
        public String getRole() { return role; }
        public String getFullName() { return fullName; }
    }

    // --- נתיב ההתחברות עצמו ---

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        try {
            // מנוע האבטחה בודק אם תעודת הזהות והסיסמה תואמים למסד הנתונים
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getIdNumber(), authRequest.getPassword())
            );
        } catch (Exception e) {
            // אם טעינו בסיסמה או בת.ז
            return ResponseEntity.status(401).body("Invalid ID Number or Password");
        }

        // אם ההתחברות הצליחה, שולפים את פרטי המשתמש כדי לבנות לו טוקן
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getIdNumber());
        User user = userRepository.findByIdNumber(authRequest.getIdNumber()).get();

        // מוציאים את התפקיד (ADMIN או CONTROLLER)
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        // מייצרים את הטוקן!
        final String jwt = jwtUtil.generateToken(userDetails.getUsername(), role);

        // מחזירים ל-React תשובה מושלמת עם הטוקן, התפקיד והשם (כדי לכתוב לו "שלום, פקח X" במסך)
        return ResponseEntity.ok(new AuthResponse(jwt, role, user.getFullName()));
    }
}