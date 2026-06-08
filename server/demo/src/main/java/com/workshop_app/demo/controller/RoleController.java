package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.RoleService;
import com.workshop_app.demo.service.dto.RoleDTO;
import com.workshop_app.demo.service.exception.DuplicateResourceException;
import com.workshop_app.demo.service.exception.InvalidRequestException;
import com.workshop_app.demo.service.exception.ResourceInUseException;
import com.workshop_app.demo.service.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/role")
@CrossOrigin
@Tag(name = "Role", description = "Role management endpoints")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "List roles", description = "Returns all roles.")
    public List<RoleDTO> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find role by id", description = "Returns a role by its id.")
    public RoleDTO findById(@Parameter(description = "Role id") @PathVariable Long id) {
        return roleService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Find role by name", description = "Returns a role by its normalized name.")
    public RoleDTO findByName(@Parameter(description = "Role name") @RequestParam String name) {
        return roleService.findByName(name);
    }

    @GetMapping("/exists")
    @Operation(summary = "Check role existence", description = "Returns true when a role exists by name.")
    public boolean existsByName(@Parameter(description = "Role name") @RequestParam String name) {
        return roleService.existsByName(name);
    }

    @PostMapping
    @Operation(summary = "Create role", description = "Creates a new role.")
    public ResponseEntity<RoleDTO> create(@RequestBody RoleDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Updates an existing role.")
    public RoleDTO update(
            @Parameter(description = "Role id") @PathVariable Long id,
            @RequestBody RoleDTO request) {
        return roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Deletes a role when it is not assigned to users.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Role id") @PathVariable Long id) {
        roleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(InvalidRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler({DuplicateResourceException.class, ResourceInUseException.class})
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException exception) {
        return error(HttpStatus.CONFLICT, exception);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
    }
}
