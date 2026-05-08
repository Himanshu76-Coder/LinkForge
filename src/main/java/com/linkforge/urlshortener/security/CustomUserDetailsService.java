package com.linkforge.urlshortener.security;

import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

// Loads the User entity from the database to serve as the Spring Security principal
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Required by UserDetailsService — wraps the User entity in a Spring Security UserDetails object.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = loadUserEntityByUsername(username);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getHashedPassword(),
                true, true, true, true,
                Collections.emptyList()
        );
    }

    // Loads the full User entity by username.
    public User loadUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Loads the full User entity by ID — called by JwtAuthenticationFilter using the userId claim.
    public User loadUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
