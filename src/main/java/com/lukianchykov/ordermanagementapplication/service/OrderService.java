package com.lukianchykov.ordermanagementapplication.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.lukianchykov.ordermanagementapplication.controller.exception.BusinessRuleException;
import com.lukianchykov.ordermanagementapplication.controller.exception.DuplicateResourceException;
import com.lukianchykov.ordermanagementapplication.controller.exception.ResourceNotFoundException;
import com.lukianchykov.ordermanagementapplication.domain.Client;
import com.lukianchykov.ordermanagementapplication.domain.Order;
import com.lukianchykov.ordermanagementapplication.dto.OrderCreateDto;
import com.lukianchykov.ordermanagementapplication.dto.OrderResponseDto;
import com.lukianchykov.ordermanagementapplication.mapper.OrderMapper;
import com.lukianchykov.ordermanagementapplication.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OrderService {

    private static final BigDecimal MIN_PROFIT_THRESHOLD = new BigDecimal("-1000");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private OrderMapper orderMapper;

    private final Random random = new Random();

    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto dto) {
        log.info("Creating new order: {}", dto.getName());

        validateOrderCreation(dto);

        Long smallerId = Math.min(dto.getSupplierId(), dto.getConsumerId());
        Long largerId = Math.max(dto.getSupplierId(), dto.getConsumerId());

        Client firstClient = clientService.getClientByIdForUpdate(smallerId);
        Client secondClient = clientService.getClientByIdForUpdate(largerId);

        Client supplier = dto.getSupplierId().equals(smallerId) ? firstClient : secondClient;
        Client consumer = dto.getSupplierId().equals(smallerId) ? secondClient : firstClient;

        if (!supplier.getActive()) {
            throw new BusinessRuleException("Cannot create order for inactive supplier: " + supplier.getName());
        }
        if (!consumer.getActive()) {
            throw new BusinessRuleException("Cannot create order for inactive consumer: " + consumer.getName());
        }

        Optional<Order> existingOrder = orderRepository.findByBusinessKeyForUpdate(
            dto.getName(), dto.getSupplierId(), dto.getConsumerId());

        if (existingOrder.isPresent()) {
            log.warn("Duplicate order detected: {}", dto.getName());
            throw new DuplicateResourceException("Order with this business key already exists");
        }

        BigDecimal newConsumerProfit = consumer.getProfit().subtract(dto.getPrice());
        if (newConsumerProfit.compareTo(MIN_PROFIT_THRESHOLD) < 0) {
            log.warn("Order {} rejected due to profit limit. Consumer: {}, Current profit: {}, Order price: {}, New profit would be: {}",
                dto.getName(), consumer.getName(), consumer.getProfit(), dto.getPrice(), newConsumerProfit);
            throw new BusinessRuleException("Order would make consumer profit less than -1000. Current: "
                + consumer.getProfit() + ", After order: " + newConsumerProfit);
        }

        supplier.setProfit(supplier.getProfit().add(dto.getPrice()));
        consumer.setProfit(newConsumerProfit);

        clientService.saveClient(supplier);
        clientService.saveClient(consumer);

        simulateProcessingDelay();

        Order order = new Order();
        order.setName(dto.getName());
        order.setSupplier(supplier);
        order.setConsumer(consumer);
        order.setPrice(dto.getPrice());
        order.setProcessingStartTime(LocalDateTime.now());
        order.setProcessingEndTime(LocalDateTime.now());

        try {
            order = orderRepository.save(order);
            log.info("Order created successfully: {} with ID: {}, Consumer new profit: {}",
                order.getName(), order.getId(), consumer.getProfit());
            return orderMapper.toOrderResponseDto(order);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate order detected at database level: {}", e.getMessage());
            throw new DuplicateResourceException("Order with this business key already exists");
        }
    }

    @Transactional
    public OrderResponseDto getOrder(Long id) {
        log.debug("Getting order with ID: {}", id);
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toOrderResponseDto(order);
    }

    @Transactional
    public List<OrderResponseDto> getAllOrders() {
        log.debug("Getting all orders");
        return orderRepository.findAll().stream()
            .map(orderMapper::toOrderResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<OrderResponseDto> getOrdersByClient(Long clientId) {
        log.debug("Getting orders for client ID: {}", clientId);
        if (!clientService.getClientById(clientId).getId().equals(clientId)) {
            throw new ResourceNotFoundException("Client not found with id: " + clientId);
        }

        return orderRepository.findByClientId(clientId).stream()
            .map(orderMapper::toOrderResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<OrderResponseDto> getSuppliedOrders(Long supplierId) {
        log.debug("Getting supplied orders for client ID: {}", supplierId);
        if (!clientService.getClientById(supplierId).getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Client not found with id: " + supplierId);
        }

        return orderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
            .map(orderMapper::toOrderResponseDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public List<OrderResponseDto> getConsumedOrders(Long consumerId) {
        log.debug("Getting consumed orders for client ID: {}", consumerId);
        if (!clientService.getClientById(consumerId).getId().equals(consumerId)) {
            throw new ResourceNotFoundException("Client not found with id: " + consumerId);
        }

        return orderRepository.findByConsumerIdOrderByCreatedAtDesc(consumerId).stream()
            .map(orderMapper::toOrderResponseDto)
            .collect(Collectors.toList());
    }

    private void validateOrderCreation(OrderCreateDto dto) {
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Order price must be positive");
        }
        if (dto.getSupplierId() == null || dto.getConsumerId() == null) {
            throw new BusinessRuleException("Supplier and consumer IDs are required");
        }
        if (dto.getSupplierId().equals(dto.getConsumerId())) {
            throw new BusinessRuleException("Supplier and consumer cannot be the same client");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessRuleException("Order name is required");
        }
    }

    private void simulateProcessingDelay() {
        try {
            int delaySeconds = random.nextInt(10) + 1;
            log.debug("Simulating order processing delay: {} seconds", delaySeconds);
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Order processing delay interrupted", e);
        }
    }
}