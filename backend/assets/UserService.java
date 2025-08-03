package com.onebox.backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.onebox.backend.DTO.UserDto;
import com.onebox.backend.Model.User;
import com.onebox.backend.Repository.UserRepository;;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

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

    public String login(UserDto userDto) {
        User user = userRepository.findByEmail(userDto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            return jwtService.generateToken(user);
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}