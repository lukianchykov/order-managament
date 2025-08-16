package com.lukianchykov.ordermanagementapplication.repository;

import java.util.List;
import java.util.Optional;

import com.lukianchykov.ordermanagementapplication.domain.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    
    List<Order> findByConsumerIdOrderByCreatedAtDesc(Long consumerId);
    
    @Query("SELECT o FROM Order o WHERE o.supplier.id = :clientId OR o.consumer.id = :clientId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT o FROM Order o WHERE o.name = :name AND o.supplier.id = :supplierId AND o.consumer.id = :consumerId")
    Optional<Order> findByBusinessKeyForUpdate(@Param("name") String name,
                                               @Param("supplierId") Long supplierId,
                                               @Param("consumerId") Long consumerId);
}