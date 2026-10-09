package com.hotel.repository;

import com.hotel.entity.Payment;
import com.hotel.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    BigDecimal sumByStatus(@Param("status") PaymentStatus status);
}
