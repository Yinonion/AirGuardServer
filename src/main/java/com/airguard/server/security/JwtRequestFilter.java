package com.airguard.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // תופסים את ההדר מהבקשה
        final String authorizationHeader = request.getHeader("Authorization");

        String idNumber = null;
        String jwt = null;

        // טוקנים סטנדרטיים נשלחים תמיד עם המילה "Bearer " לפני המחרוזת
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // חותכים את הקידומת כדי לקבל רק את הטוקן
            try {
                idNumber = jwtUtil.extractIdNumber(jwt);
            } catch (Exception e) {
                System.out.println("Invalid or Expired JWT Token");
            }
        }

        // אם מצאנו ת.ז והמשתמש עדיין לא אושר בבקשה הנוכחית
        if (idNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // מושכים את הפרטים שלו מהמסד
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(idNumber);

            // בודקים מול JwtUtil אם הטוקן באמת תקין ותואם
            if (jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {

                // אם הכל תקין, אנחנו אומרים ל-Spring "אפשר להכניס אותו, הוא משלנו"
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // מעבירים את הבקשה הלאה לתחנה הבאה
        chain.doFilter(request, response);
    }
}