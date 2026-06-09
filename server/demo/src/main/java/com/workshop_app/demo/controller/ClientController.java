package com.workshop_app.demo.controller;

import com.workshop_app.demo.service.ClientService;
import com.workshop_app.demo.service.dto.ClientDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client")
@CrossOrigin
@Tag(name = "Client", description = "Client management endpoints")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "List clients", description = "Returns all clients.")
    public List<ClientDTO> findAll() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find client by id", description = "Returns a client by id.")
    public ClientDTO findById(@Parameter(description = "Client id") @PathVariable Long id) {
        return clientService.findById(id);
    }

    @GetMapping("/search/email")
    @Operation(summary = "Find clients by email", description = "Returns clients matching an email.")
    public List<ClientDTO> findByEmail(@Parameter(description = "Client email") @RequestParam String email) {
        return clientService.findByEmail(email);
    }

    @GetMapping("/search/phone")
    @Operation(summary = "Find clients by phone", description = "Returns clients matching a phone number.")
    public List<ClientDTO> findByPhone(@Parameter(description = "Client phone") @RequestParam String phone) {
        return clientService.findByPhone(phone);
    }

    @GetMapping("/search/name")
    @Operation(summary = "Find clients by name", description = "Returns clients matching a name.")
    public List<ClientDTO> findByName(@Parameter(description = "Client name") @RequestParam String name) {
        return clientService.findByName(name);
    }

    @GetMapping("/exists/email")
    @Operation(summary = "Check client email existence", description = "Returns true when a client exists by email.")
    public boolean existsByEmail(@Parameter(description = "Client email") @RequestParam String email) {
        return clientService.existsByEmail(email);
    }

    @GetMapping("/exists/phone")
    @Operation(summary = "Check client phone existence", description = "Returns true when a client exists by phone.")
    public boolean existsByPhone(@Parameter(description = "Client phone") @RequestParam String phone) {
        return clientService.existsByPhone(phone);
    }

    @GetMapping("/exists/name")
    @Operation(summary = "Check client name existence", description = "Returns true when a client exists by name.")
    public boolean existsByName(@Parameter(description = "Client name") @RequestParam String name) {
        return clientService.existsByName(name);
    }

    @PostMapping
    @Operation(summary = "Create client", description = "Creates a new client.")
    public ResponseEntity<ClientDTO> create(@RequestBody ClientDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client", description = "Updates an existing client.")
    public ClientDTO update(
            @Parameter(description = "Client id") @PathVariable Long id,
            @RequestBody ClientDTO request) {
        return clientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client", description = "Deletes a client when no repair orders or installments depend on it.")
    public ResponseEntity<Void> deleteById(@Parameter(description = "Client id") @PathVariable Long id) {
        clientService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
