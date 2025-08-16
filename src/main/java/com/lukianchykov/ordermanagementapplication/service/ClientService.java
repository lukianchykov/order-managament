package com.lukianchykov.ordermanagementapplication.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.lukianchykov.ordermanagementapplication.controller.exception.DuplicateResourceException;
import com.lukianchykov.ordermanagementapplication.controller.exception.ResourceNotFoundException;
import com.lukianchykov.ordermanagementapplication.domain.Client;
import com.lukianchykov.ordermanagementapplication.dto.ClientCreateDto;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import com.lukianchykov.ordermanagementapplication.mapper.ClientMapper;
import com.lukianchykov.ordermanagementapplication.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientMapper clientMapper;

    @Transactional
    public ClientResponseDto createClient(ClientCreateDto dto) {
        log.info("Creating new client with email: {}", dto.getEmail());

        if (clientRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Client with email " + dto.getEmail() + " already exists");
        }

        Client client = new Client();
        client.setName(dto.getName());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());
        client.setPhone(dto.getPhone());

        client = clientRepository.save(client);
        log.info("Client created successfully with ID: {}", client.getId());

        return convertToResponseDto(client);
    }

    @Transactional
    public ClientResponseDto getClient(Long id) {
        log.debug("Getting client with ID: {}", id);
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        return convertToResponseDto(client);
    }

    @Transactional
    public List<ClientResponseDto> getAllClients() {
        log.debug("Getting all clients");
        return clientRepository.findAll().stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<ClientResponseDto> searchClients(String keyword) {
        log.info("Searching clients by keyword: '{}'", keyword);

        if (keyword == null || keyword.trim().length() < 3) {
            log.warn("Search keyword is too short (minimum 3 characters required): '{}'", keyword);
            throw new IllegalArgumentException("Search keyword must be at least 3 characters long");
        }

        String trimmedKeyword = keyword.trim();
        List<Client> clients = clientRepository.findByKeyword(trimmedKeyword);

        log.info("Found {} clients matching keyword: '{}'", clients.size(), trimmedKeyword);

        return clients.stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponseDto updateClient(Long id, ClientCreateDto dto) {
        log.info("Updating client with ID: {}", id);
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        if (!client.getEmail().equals(dto.getEmail()) &&
            clientRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Client with email " + dto.getEmail() + " already exists");
        }

        client.setName(dto.getName());
        client.setEmail(dto.getEmail());
        client.setAddress(dto.getAddress());
        client.setPhone(dto.getPhone());

        client = clientRepository.save(client);
        log.info("Client updated successfully with ID: {}", client.getId());

        return convertToResponseDto(client);
    }

    public ClientResponseDto deactivateClient(Long id) {
        log.info("Deactivating client with ID: {}", id);
        Client client = clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        if (!client.getActive()) {
            throw new IllegalArgumentException("Client is already inactive");
        }

        client.setActive(false);
        client.setDeactivatedAt(LocalDateTime.now());

        client = clientRepository.save(client);
        log.info("Client deactivated successfully with ID: {}", client.getId());

        return convertToResponseDto(client);
    }

    @Transactional
    public BigDecimal getClientProfit(Long clientId) {
        log.debug("Calculating profit for client ID: {}", clientId);
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with id: " + clientId);
        }

        Client client = getClientById(clientId);
        return client.getProfit() != null ? client.getProfit() : BigDecimal.ZERO;
    }

    @Transactional
    public List<ClientResponseDto> getClientsByProfitRange(BigDecimal minProfit, BigDecimal maxProfit) {
        log.debug("Getting clients with profit range: {} to {}", minProfit, maxProfit);
        List<Client> clients = clientRepository.findByProfitBetween(minProfit, maxProfit);

        return clients.stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    public Client getClientByIdForUpdate(Long id) {
        return clientRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    @Transactional
    public void saveClient(Client client) {
        clientRepository.save(client);
    }


    private ClientResponseDto convertToResponseDto(Client client) {
        ClientResponseDto dto = clientMapper.toClientResponseDto(client);
        dto.setTotalProfit(client.getProfit() != null ? client.getProfit() : BigDecimal.ZERO);
        return dto;
    }
}