package com.onebox.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.onebox.backend.DTO.UserDto;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.Jpa.UserRepository;
import com.onebox.backend.Utils.JwtResponse;;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;

    public ResponseEntity<?> login(UserDto dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
        final String jwt = jwtService.generateTokens(userDetails.getUsername());

        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    public User createUser(UserDto userDto) {
        String email = userDto.getEmail();
        String password = userDto.getPassword();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists");
        }

        User newUser = new User();
        newUser.setEmail(email);

        String hashedPassword = passwordEncoder.encode(password);
        newUser.setPassword(hashedPassword);

        return userRepository.save(newUser);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}