package com.lukianchykov.ordermanagementapplication.mapper;

import com.lukianchykov.ordermanagementapplication.domain.Client;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    
    @Mapping(target = "totalProfit", ignore = true)
    ClientResponseDto toClientResponseDto(Client client);
}