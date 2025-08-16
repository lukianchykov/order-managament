package com.lukianchykov.ordermanagementapplication;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import com.lukianchykov.ordermanagementapplication.dto.OrderCreateDto;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class ProfitResetApplication {

    private static final String BASE_URL = "http://localhost:8080/api";

    public static void main(String[] args) {
        SpringApplication.run(ProfitResetApplication.class, args);
    }

    @Bean
    public CommandLineRunner resetAllClientsProfits() {
        return args -> {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

            log.info("Starting profit reset for all clients...");

            try {
                ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/clients", String.class);
                List<ClientResponseDto> clients = objectMapper.readValue(response.getBody(),
                    new TypeReference<>() {});

                log.info("Found {} clients to process", clients.size());

                ClientResponseDto systemClient = createSystemClient(restTemplate);

                int processedCount = 0;
                int skippedCount = 0;

                for (ClientResponseDto client : clients) {
                    if (!client.getId().equals(systemClient.getId()) && client.getActive()) {
                        BigDecimal currentProfit = client.getTotalProfit();

                        if (currentProfit.compareTo(BigDecimal.ZERO) == 0) {
                            log.info("Client {}: already has zero profit, skipping", client.getName());
                            skippedCount++;
                            continue;
                        }

                        log.info("Client {}: current profit = {}", client.getName(), currentProfit);

                        if (currentProfit.compareTo(BigDecimal.ZERO) > 0) {
                            createOffsetOrder(restTemplate, systemClient.getId(), client.getId(),
                                currentProfit, "CLIENT_POSITIVE_RESET");
                        } else {
                            // Отрицательная прибыль - делаем клиента поставщиком
                            createOffsetOrder(restTemplate, client.getId(), systemClient.getId(),
                                currentProfit.abs(), "CLIENT_NEGATIVE_RESET");
                        }

                        processedCount++;
                        log.info("Client {}: profit reset completed", client.getName());
                    }
                }

                log.info("=== PROFIT RESET SUMMARY ===");
                log.info("Processed clients: {}", processedCount);
                log.info("Skipped clients (zero profit): {}", skippedCount);
                log.info("System client: {}", systemClient.getName());

                verifyResults(restTemplate, objectMapper);

            } catch (Exception e) {
                log.error("Error resetting client profits", e);
            }
        };
    }

    private ClientResponseDto createSystemClient(RestTemplate restTemplate) {
        try {
            String uniqueEmail = "system.profit.reset." + System.currentTimeMillis() + "@system.local";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String clientJson = """
                {
                    "name": "SYSTEM_PROFIT_RESET_CLIENT",
                    "email": "%s",
                    "address": "System Generated for Profit Reset",
                    "phone": "+000-SYSTEM-RESET"
                }
                """.formatted(uniqueEmail);

            HttpEntity<String> request = new HttpEntity<>(clientJson, headers);

            ResponseEntity<ClientResponseDto> response = restTemplate.postForEntity(
                BASE_URL + "/clients", request, ClientResponseDto.class);

            ClientResponseDto systemClient = response.getBody();
            log.info("Created system client: {} (ID: {})", systemClient.getName(), systemClient.getId());
            return systemClient;

        } catch (Exception e) {
            log.error("Failed to create system client", e);
            throw new RuntimeException("Cannot create system client for profit reset", e);
        }
    }

    private void createOffsetOrder(RestTemplate restTemplate, Long supplierId, Long consumerId,
                                   BigDecimal price, String orderType) {
        try {
            OrderCreateDto orderDto = new OrderCreateDto();
            orderDto.setName(orderType + "_" + consumerId + "_" + System.currentTimeMillis());
            orderDto.setSupplierId(supplierId);
            orderDto.setConsumerId(consumerId);
            orderDto.setPrice(price);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderCreateDto> request = new HttpEntity<>(orderDto, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/orders", request, String.class);

            log.debug("Created offset order: {} -> {} amount {} ({})",
                supplierId, consumerId, price, response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to create offset order: supplier {}, consumer {}, amount {}",
                supplierId, consumerId, price, e);
            throw new RuntimeException("Cannot create offset order", e);
        }
    }

    private void verifyResults(RestTemplate restTemplate, ObjectMapper objectMapper) {
        try {
            log.info("=== VERIFYING RESULTS ===");

            ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/clients", String.class);
            List<ClientResponseDto> clients = objectMapper.readValue(response.getBody(),
                new TypeReference<>() {});

            int zeroProfit = 0;
            int nonZeroProfit = 0;
            BigDecimal totalProfit = BigDecimal.ZERO;

            for (ClientResponseDto client : clients) {
                if (client.getActive() && !client.getName().startsWith("SYSTEM_")) {
                    BigDecimal profit = client.getTotalProfit();
                    totalProfit = totalProfit.add(profit);

                    if (profit.compareTo(BigDecimal.ZERO) == 0) {
                        zeroProfit++;
                    } else {
                        nonZeroProfit++;
                        log.warn("Client {} still has non-zero profit: {}", client.getName(), profit);
                    }
                }
            }

            log.info("Clients with zero profit: {}", zeroProfit);
            log.info("Clients with non-zero profit: {}", nonZeroProfit);
            log.info("Total profit across all clients: {}", totalProfit);

            if (nonZeroProfit == 0) {
                log.info("SUCCESS: All client profits have been reset to zero!");
            } else {
                log.warn("WARNING: {} clients still have non-zero profits", nonZeroProfit);
            }

        } catch (Exception e) {
            log.error("Error verifying results", e);
        }
    }
}