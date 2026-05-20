package com.airguard.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // מפתח סודי שרק השרת מכיר. איתו הוא "חותם" על הטוקנים כדי שאי אפשר יהיה לזייף אותם.
    // במערכת אמיתית זה יושב בקובץ הגדרות מוסתר, פה נשים את זה ישירות לצורך הנוחות.
    private static final String SECRET_KEY = "AirGuardTopSecretKeyForJwtAuthenticationMustBeLongEnough";

    // תוקף הטוקן - נגדיר ל-10 שעות (אורך של משמרת פקח טיסה)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // הפונקציה שמייצרת את הטוקן למשתמש אחרי שהתחבר בהצלחה
    public String generateToken(String idNumber, String role) {
        return Jwts.builder()
                .setSubject(idNumber) // מכניסים את תעודת הזהות בתור ה"נושא" של הטוקן
                .claim("role", role)  // מכניסים את התפקיד (Admin/Controller)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // חותמים עם המפתח הסודי
                .compact();
    }

    // חילוץ תעודת הזהות מתוך טוקן קיים
    public String extractIdNumber(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // חילוץ התפקיד מתוך טוקן קיים
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    // בדיקה האם הטוקן עדיין בתוקף ושייך למשתמש הנכון
    public boolean isTokenValid(String token, String idNumber) {
        final String extractedId = extractIdNumber(token);
        return (extractedId.equals(idNumber) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}