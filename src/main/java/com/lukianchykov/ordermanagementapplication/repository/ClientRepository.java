package com.lukianchykov.ordermanagementapplication.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.lukianchykov.ordermanagementapplication.domain.Client;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    @Query("SELECT c FROM Client c WHERE " +
        "LENGTH(:keyword) >= 3 AND (" +
        "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(COALESCE(c.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
        ")")
    List<Client> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Client c WHERE c.profit BETWEEN :minProfit AND :maxProfit")
    List<Client> findByProfitBetween(@Param("minProfit") BigDecimal minProfit,
                                     @Param("maxProfit") BigDecimal maxProfit);

    @Query("SELECT c FROM Client c WHERE c.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Client> findByIdForUpdate(@Param("id") Long id);
}