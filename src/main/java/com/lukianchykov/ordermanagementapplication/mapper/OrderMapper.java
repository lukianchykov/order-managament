package com.lukianchykov.ordermanagementapplication.mapper;

import com.lukianchykov.ordermanagementapplication.domain.Client;
import com.lukianchykov.ordermanagementapplication.domain.Order;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import com.lukianchykov.ordermanagementapplication.dto.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "supplier", source = "supplier")
    @Mapping(target = "consumer", source = "consumer")
    OrderResponseDto toOrderResponseDto(Order order);
    
    @Mapping(target = "totalProfit", ignore = true)
    ClientResponseDto toClientResponseDto(Client client);
}