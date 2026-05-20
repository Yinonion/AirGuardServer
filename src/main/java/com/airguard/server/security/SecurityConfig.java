package com.airguard.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // הגדרת מצפין הסיסמאות (BCrypt הוא הסטנדרט כיום, נשתמש בו כשניצור משתמשים)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // מכבים CSRF כי אנחנו משתמשים ב-JWT
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // מאפשרים ל-React לגשת
                .authorizeHttpRequests(auth -> auth
                        // 1. נתיב ההתחברות פתוח לכולם (אחרת אי אפשר לעשות לוגין!)
                        .requestMatchers("/api/auth/login").permitAll()

                        // 2. 🌟 הגדרת הרשאות חכמות לפי הבקשה שלך 🌟
                        // רק ADMIN יכול לעצור/לחדש יצירת מטוסים:
                        .requestMatchers("/api/toggle-spawn").hasRole("ADMIN")

                        // רק ADMIN יכול לגשת לנתיבי ניהול משתמשים (נוסיף את הנתיב הזה בהמשך בשביל הוספת בקרים):
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // שאר הפעולות במערכת (מטוסים, רדאר וכו') מותרות גם לאדמין וגם לבקר:
                        .requestMatchers("/api/planes/**").hasAnyRole("ADMIN", "CONTROLLER")

                        // כל בקשה אחרת דורשת לפחות להיות מחובר:
                        .anyRequest().authenticated()
                )
                // אנחנו עובדים בשיטת Stateless (בלי סשן בשרת, רק טוקנים)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // מוסיפים את המסננת שלנו שתבדוק את הטוקן לפני כל בקשה
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // הגדרות CORS כדי שה-React (שעובד על פורט 3000) לא ייחסם על ידי ה-Java (פורט 8080)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // כתובת ה-React
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}