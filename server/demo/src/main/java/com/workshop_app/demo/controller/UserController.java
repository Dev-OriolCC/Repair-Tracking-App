package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.UserService;
import com.workshop_app.demo.service.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin
@Tag(name = "User", description = "User management endpoints")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Returns all users.")
    public List<UserDTO> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find user by id", description = "Returns a user by id.")
    public UserDTO findById(@Parameter(description = "User id") @PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Find user by email", description = "Returns a user by email.")
    public UserDTO findByEmail(@Parameter(description = "User email") @RequestParam String email) {
        return userService.findByEmail(email);
    }

    @GetMapping("/exists")
    @Operation(summary = "Check user email existence", description = "Returns true when a user exists by email.")
    public boolean existsByEmail(@Parameter(description = "User email") @RequestParam String email) {
        return userService.existsByEmail(email);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user.")
    public UserDTO update(
            @Parameter(description = "User id") @PathVariable Long id,
            @RequestBody UserDTO request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user when no repair orders or installments depend on it.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "User id") @PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
