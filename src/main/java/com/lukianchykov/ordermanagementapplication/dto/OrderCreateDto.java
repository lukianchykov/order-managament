package com.lukianchykov.ordermanagementapplication.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateDto {

    @NotBlank(message = "Order name is required")
    private String name;

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Consumer ID is required")
    private Long consumerId;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
}