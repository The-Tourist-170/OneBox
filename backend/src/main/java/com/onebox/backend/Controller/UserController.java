package com.onebox.backend.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.onebox.backend.DTO.UserDto;
import com.onebox.backend.Model.User;
import com.onebox.backend.Service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // @PostMapping("/login")
    // public ResponseEntity<?> loginUser(@RequestBody UserDto dto) {
    // return ResponseEntity.ok(userService.login(dto));
    // }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto dto) {
        return userService.login(dto);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDto dto) {
        try {
            User newUser = userService.createUser(dto);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("id", newUser.getId());
            responseMap.put("email", newUser.getEmail());
            responseMap.put("message", "User registered successfully");
            return ResponseEntity.ok(responseMap);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
