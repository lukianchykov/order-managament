package com.lukianchykov.ordermanagementapplication.controller;

import java.math.BigDecimal;
import java.util.List;

import com.lukianchykov.ordermanagementapplication.dto.ClientCreateDto;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import com.lukianchykov.ordermanagementapplication.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Client Management", description = "Operations for managing clients")
public class ClientController {
    
    @Autowired
    private ClientService clientService;
    
    @Operation(summary = "Create new client")
    @PostMapping
    public ResponseEntity<ClientResponseDto> createClient(@Valid @RequestBody ClientCreateDto dto) {
        ClientResponseDto response = clientService.createClient(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Get client by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDto> getClient(@PathVariable Long id) {
        ClientResponseDto response = clientService.getClient(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all clients")
    @GetMapping
    public ResponseEntity<List<ClientResponseDto>> getAllClients() {
        List<ClientResponseDto> response = clientService.getAllClients();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search clients by keyword")
    @GetMapping("/search")
    public ResponseEntity<List<ClientResponseDto>> searchClients(
        @Parameter(description = "Search keyword (minimum 3 characters)", example = "john")
        @RequestParam String keyword) {
        List<ClientResponseDto> response = clientService.searchClients(keyword);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update client")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDto> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientCreateDto dto) {
        ClientResponseDto response = clientService.updateClient(id, dto);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Deactivate client")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ClientResponseDto> deactivateClient(@PathVariable Long id) {
        ClientResponseDto response = clientService.deactivateClient(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get client total profit")
    @GetMapping("/{id}/profit")
    public ResponseEntity<BigDecimal> getClientProfit(@PathVariable Long id) {
        BigDecimal profit = clientService.getClientProfit(id);
        return ResponseEntity.ok(profit);
    }
    
    @Operation(summary = "Get clients by profit range")
    @GetMapping("/profit-range")
    public ResponseEntity<List<ClientResponseDto>> getClientsByProfitRange(
            @Parameter(description = "Minimum profit") @RequestParam BigDecimal minProfit,
            @Parameter(description = "Maximum profit") @RequestParam BigDecimal maxProfit) {
        List<ClientResponseDto> response = clientService.getClientsByProfitRange(minProfit, maxProfit);
        return ResponseEntity.ok(response);
    }
}