package com.lukianchykov.ordermanagementapplication.controller;

import java.util.List;

import com.lukianchykov.ordermanagementapplication.dto.OrderCreateDto;
import com.lukianchykov.ordermanagementapplication.dto.OrderResponseDto;
import com.lukianchykov.ordermanagementapplication.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Operations for managing orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Operation(summary = "Create new order")
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateDto dto) {
        OrderResponseDto response = orderService.createOrder(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long id) {
        OrderResponseDto response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get all orders")
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        List<OrderResponseDto> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get orders by client (both supplied and consumed)")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByClient(@PathVariable Long clientId) {
        List<OrderResponseDto> response = orderService.getOrdersByClient(clientId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get orders supplied by client")
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<OrderResponseDto>> getSuppliedOrders(@PathVariable Long supplierId) {
        List<OrderResponseDto> response = orderService.getSuppliedOrders(supplierId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get orders consumed by client")
    @GetMapping("/consumer/{consumerId}")
    public ResponseEntity<List<OrderResponseDto>> getConsumedOrders(@PathVariable Long consumerId) {
        List<OrderResponseDto> response = orderService.getConsumedOrders(consumerId);
        return ResponseEntity.ok(response);
    }
}