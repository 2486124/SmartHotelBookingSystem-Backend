package com.cts.project.shbs.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.cts.project.shbs.model.User;
import com.cts.project.shbs.repository.UserRepository;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}")
    private int jwtExpirationMs;
    
    @Autowired
    private UserRepository userRepository; // Add this!

    // Generates a secure key using the secret from application.properties
    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        
        // 1. Get the userId and role from Spring Security
        Long userId = Long.parseLong(userPrincipal.getUsername());
        String role = userPrincipal.getAuthorities().iterator().next().getAuthority();

        // 2. Fetch your actual User entity from the database using the email
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Build the perfect token
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId())) // subject = userId (integer)
                .claim("role", role)          // Claim is "ROLE_HOTEL_MANAGER"
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extracts userId (subject) from the token
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
    
    // Validates the token's signature and expiration
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }
    
 // Generates a 15-minute password reset token
    public String generatePasswordResetToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("purpose", "password-reset")
                .claim("oldPasswordHash", user.getPassword()) // for single-use validation
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 900000)) // 15 mins
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validates reset token and returns claims if valid
    public Claims validatePasswordResetToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Ensure this token was meant for password reset only
        String purpose = claims.get("purpose", String.class);
        if (!"password-reset".equals(purpose)) {
            throw new JwtException("Invalid token purpose");
        }
        return claims;
    }
}