package com.tradex.service;

import com.tradex.model.User;
import com.tradex.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * H2 Database-backed user store. Supports registration and lookup.
 * Pre-loaded with a default admin user.
 */
@Service
public class UserStore implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserStore(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // Default user
        if (!userExists("admin")) {
            register("admin", "password");
        }
    }

    public boolean register(String username, String password) {
        if (userExists(username)) {
            return false;
        }
        User newUser = new User(username, passwordEncoder.encode(password), "ROLE_USER");
        userRepository.save(newUser);
        return true;
    }

    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}
