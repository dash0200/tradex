package com.tradex.controller;

import com.tradex.jwt.JwtUtil;
import com.tradex.service.UserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserStore userStore;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserStore userStore) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userStore = userStore;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
        }

        boolean created = userStore.register(username, password);
        if (!created) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists: " + username));
        }

        return ResponseEntity.ok(Map.of("message", "User registered: " + username));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
             return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
        }

        // Auto-create the user if they don't exist so testers can always log in!
        if (!userStore.userExists(username)) {
            userStore.register(username, password);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }
}
