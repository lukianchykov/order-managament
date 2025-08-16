package com.lukianchykov.ordermanagementapplication;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.lukianchykov.ordermanagementapplication.dto.ClientCreateDto;
import com.lukianchykov.ordermanagementapplication.dto.ClientResponseDto;
import com.lukianchykov.ordermanagementapplication.dto.OrderCreateDto;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootApplication
public class ScenarioTestApplication {

    private static final String BASE_URL = "http://localhost:8080/api";

    public static void main(String[] args) {
        SpringApplication.run(ScenarioTestApplication.class, args);
    }

    @Bean
    public CommandLineRunner runScenarios() {
        return args -> {
            RestTemplate restTemplate = new RestTemplate();

            log.info("Starting scenario tests...");
            Thread.sleep(5000);

            runScenario1(restTemplate);
            Thread.sleep(2000);

            runScenario2(restTemplate);
            Thread.sleep(2000);

            runScenario3(restTemplate);
        };
    }

    /**
     * Сценарий 1: N+1 одинаковых заказов с ценой 1
     * Ожидается: только один заказ создастся успешно
     */
    private void runScenario1(RestTemplate restTemplate) {
        log.info("=== Running Scenario 1: Duplicate orders with same price ===");

        try {
            ClientResponseDto supplier = createClient(restTemplate, "Supplier1", "supplier1@test.com");
            ClientResponseDto consumer = createClient(restTemplate, "Consumer1", "consumer1@test.com");

            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                int requestNum = i + 1;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateDto orderDto = new OrderCreateDto();
                        orderDto.setName("Similar Order");
                        orderDto.setSupplierId(supplier.getId());
                        orderDto.setConsumerId(consumer.getId());
                        orderDto.setPrice(new BigDecimal("1.00"));

                        createOrder(restTemplate, orderDto, "Request " + requestNum);
                    } catch (Exception e) {
                        log.info("Request {} failed as expected: {}", requestNum, e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            completionLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            log.info("=== Scenario 1 completed ===");

        } catch (Exception e) {
            log.error("Error in Scenario 1", e);
        }
    }

    /**
     * Сценарий 2: 10 заказов с уменьшающейся ценой при лимите прибыли
     * У покупателя profit = -970, лимит = 1000, доступно для трат = 30
     * ДОЛЖЕН создаться только ОДИН заказ из всех возможных
     */
    private void runScenario2(RestTemplate restTemplate) {
        log.info("=== Running Scenario 2: Orders with decreasing price near profit limit ===");
        log.info("EXPECTED: Only 1 order should be created due to profit limit constraint");

        try {
            ClientResponseDto supplier = createClient(restTemplate, "Supplier2", "supplier2@test.com");
            ClientResponseDto consumer = createClient(restTemplate, "Consumer2", "consumer2@test.com");

            log.info("Creating setup order to reach profit limit...");
            createOrderForConsumer(restTemplate, supplier.getId(), consumer.getId(),
                "Setup Order", new BigDecimal("970.00"));

            log.info("Setup completed. Consumer profit: -970, Available budget: 30");

            Thread.sleep(1000);

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            final AtomicInteger acceptedCount = new AtomicInteger(0);
            final AtomicInteger rejectedCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                int price = 100 - (i * 10);
                int orderNum = i + 1;

                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateDto orderDto = new OrderCreateDto();
                        orderDto.setName("Decreasing Order " + orderNum);
                        orderDto.setSupplierId(supplier.getId());
                        orderDto.setConsumerId(consumer.getId());
                        orderDto.setPrice(new BigDecimal(price));

                        createOrder(restTemplate, orderDto,
                            "Order " + orderNum + " (price: " + price + ")");

                        acceptedCount.incrementAndGet();
                        log.info("Order {} with price {} ACCEPTED", orderNum, price);

                    } catch (Exception e) {
                        rejectedCount.incrementAndGet();
                        log.info("Order {} with price {} REJECTED: {}", orderNum, price, e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            log.info("Starting concurrent order creation...");
            startLatch.countDown();
            completionLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            log.info("=== Scenario 2 Results ===");
            log.info("Accepted orders: {}", acceptedCount.get());
            log.info("Rejected orders: {}", rejectedCount.get());

        } catch (Exception e) {
            log.error("Error in Scenario 2", e);
        }
    }

    /**
     * Сценарий 3: Создание заказов параллельно с деактивацией клиента
     * Ожидается: создадутся только те заказы, которые были обработаны до деактивации
     */
    private void runScenario3(RestTemplate restTemplate) {
        log.info("=== Running Scenario 3: Orders creation with client deactivation ===");

        try {
            ClientResponseDto supplier = createClient(restTemplate, "Supplier3", "supplier3@test.com");
            ClientResponseDto consumer = createClient(restTemplate, "Consumer3", "consumer3@test.com");

            int orderCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(orderCount + 1);
            ExecutorService executor = Executors.newFixedThreadPool(orderCount + 1);

            for (int i = 0; i < orderCount; i++) {
                int orderNum = i + 1;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Thread.sleep(orderNum * 10);

                        OrderCreateDto orderDto = new OrderCreateDto();
                        orderDto.setName("Order " + orderNum);
                        orderDto.setSupplierId(supplier.getId());
                        orderDto.setConsumerId(consumer.getId());
                        orderDto.setPrice(new BigDecimal("50.00"));

                        createOrder(restTemplate, orderDto, "Order " + orderNum);
                    } catch (Exception e) {
                        log.info("Order {} failed (possibly due to client deactivation): {}",
                            orderNum, e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            executor.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(50);
                    deactivateClient(restTemplate, consumer.getId());
                } catch (Exception e) {
                    log.error("Failed to deactivate client", e);
                } finally {
                    completionLatch.countDown();
                }
            });

            startLatch.countDown();
            completionLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            log.info("=== Scenario 3 completed ===");

        } catch (Exception e) {
            log.error("Error in Scenario 3", e);
        }
    }

    private void deactivateClient(RestTemplate restTemplate, Long clientId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/clients/" + clientId + "/deactivate",
                HttpMethod.POST, request, String.class);

            log.info("Client {} deactivated: {}", clientId, response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to deactivate client {}", clientId, e);
            throw e;
        }
    }

    private ClientResponseDto createClient(RestTemplate restTemplate, String name, String email) {
        try {
            ClientCreateDto clientDto = new ClientCreateDto();
            clientDto.setName(name);
            clientDto.setEmail(email);
            clientDto.setAddress("Test Address");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ClientCreateDto> request = new HttpEntity<>(clientDto, headers);

            ResponseEntity<ClientResponseDto> response = restTemplate.postForEntity(
                BASE_URL + "/clients", request, ClientResponseDto.class);

            log.info("Created client: {} with ID: {}", name, response.getBody().getId());
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to create client: {}", name, e);
            throw new RuntimeException("Failed to create client", e);
        }
    }

    private void createOrderForConsumer(RestTemplate restTemplate, Long supplierId, Long consumerId,
                                        String orderName, BigDecimal price) {
        try {
            OrderCreateDto orderDto = new OrderCreateDto();
            orderDto.setName(orderName);
            orderDto.setSupplierId(supplierId);
            orderDto.setConsumerId(consumerId);
            orderDto.setPrice(price);

            createOrder(restTemplate, orderDto, orderName);
        } catch (Exception e) {
            log.error("Failed to create setup order", e);
        }
    }

    private void createOrder(RestTemplate restTemplate, OrderCreateDto orderDto, String description) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderCreateDto> request = new HttpEntity<>(orderDto, headers);

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/orders", request, String.class);
            long endTime = System.currentTimeMillis();

            log.info("Order created successfully [{}]: {} (took {}ms)",
                description, response.getStatusCode(), (endTime - startTime));

        } catch (Exception e) {
            log.warn("Order creation failed [{}]: {}", description, e.getMessage());
            throw e;
        }
    }
}