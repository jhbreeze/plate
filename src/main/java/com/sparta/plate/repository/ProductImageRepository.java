package com.sparta.plate.repository;

import com.sparta.plate.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID>, ProductImageRepositoryCustom {
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isDeleted = false")
    List<ProductImage> findActiveImagesByProductId(@Param("productId") UUID productId);
}
