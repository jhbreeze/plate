package com.sparta.plate.repository;

import com.sparta.plate.entity.Payment;
import com.sparta.plate.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByPayment(Payment payment);
}
