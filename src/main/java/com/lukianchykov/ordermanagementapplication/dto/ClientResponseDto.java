package com.lukianchykov.ordermanagementapplication.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientResponseDto {

    private Long id;

    private String name;

    private String email;

    private String address;

    private String phone;

    private BigDecimal totalProfit;

    private Boolean active;

    private LocalDateTime deactivatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}