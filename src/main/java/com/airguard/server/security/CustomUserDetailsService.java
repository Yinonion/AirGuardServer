package com.airguard.server.security;

import com.airguard.server.entity.User;
import com.airguard.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String idNumber) throws UsernameNotFoundException {
        // מחפשים את המשתמש ב-DB לפי תעודת הזהות שלו
        User user = userRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + idNumber));

        // אנחנו מוסיפים קידומת "ROLE_" כי ככה Spring Security מצפה לזה (למשל: ROLE_ADMIN)
        return new org.springframework.security.core.userdetails.User(
                user.getIdNumber(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()))
        );
    }
}