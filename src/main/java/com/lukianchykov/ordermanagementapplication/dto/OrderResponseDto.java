package com.lukianchykov.ordermanagementapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {

    private Long id;

    private String name;

    private ClientResponseDto supplier;

    private ClientResponseDto consumer;

    private BigDecimal price;

    private LocalDateTime processingStartTime;

    private LocalDateTime processingEndTime;

    private LocalDateTime createdAt;
}