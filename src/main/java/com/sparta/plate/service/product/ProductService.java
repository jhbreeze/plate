package com.sparta.plate.service.product;

import com.sparta.plate.dto.request.*;
import com.sparta.plate.dto.response.ProductImageResponseDto;
import com.sparta.plate.dto.response.ProductResponseDto;
import com.sparta.plate.entity.Product;
import com.sparta.plate.entity.ProductDisplayStatusEnum;
import com.sparta.plate.entity.ProductImage;
import com.sparta.plate.entity.Store;
import com.sparta.plate.exception.*;
import com.sparta.plate.repository.ProductRepository;
import com.sparta.plate.security.UserDetailsImpl;
import com.sparta.plate.service.store.GetStoreService;
import com.sparta.plate.util.PageableUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService imageService;
    private final ProductOwnershipService productOwnershipService;
    private final ProductHistoryService historyService;
    private final GetStoreService storeService;

    @Transactional
    public UUID createProduct(ProductRequestDto requestDto, UserDetailsImpl userDetails) throws IOException {
        Store store = storeService.getStore(requestDto.getStoreId());
        Product product = Product.toEntity(requestDto, store);

        String storeUserId = store.getUser().getId().toString();
        String userId = userDetails.getUser().getId().toString();
        if (!storeUserId.equals(userId)) {
            throw new StoreOwnerMismatchException("You are not the owner of this product");
        }

        if (requestDto.getImages() != null) {
            List<ProductImage> newImages = imageService.processProductImages(product, requestDto.getImages());

            product.setProductImages(newImages);
        }

        Product savedProduct = productRepository.saveAndFlush(product);

        ProductDetailsRequestDto currentDto = new ProductDetailsRequestDto(product.getName(), product.getDescription(), product.getPrice());
        historyService.createProductHistory(currentDto, savedProduct.getId());

        return savedProduct.getId();
    }

    @Transactional
    public void deleteProduct(UUID productId, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());

        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        product.markAsDeleted(userDetails.getUser().getId());
        productRepository.save(product);
    }

    @Transactional
    public void updateProductDetails(UUID productId, ProductDetailsRequestDto requestDto, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());
        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        requestDto.setProductName(requestDto.getProductName() == null ? product.getName() : requestDto.getProductName());
        product.setName(requestDto.getProductName() != null ? requestDto.getProductName() : product.getName());

        requestDto.setProductDescription(requestDto.getProductDescription() == null ? product.getDescription() : requestDto.getProductDescription());
        product.setDescription(requestDto.getProductDescription() != null ? requestDto.getProductDescription() : product.getDescription());

        requestDto.setPrice(requestDto.getPrice() == null ? product.getPrice() : requestDto.getPrice());
        product.setPrice(requestDto.getPrice() != null ? requestDto.getPrice() : product.getPrice());

        productRepository.saveAndFlush(product);

        historyService.createProductHistory(requestDto, productId);
    }

    @Transactional
    public void updateStockAndLimit(UUID productId, ProductQuantityRequestDto requestDto, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());
        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        product.setMaxOrderLimit(requestDto.getMaxOrderLimit() != null ? requestDto.getMaxOrderLimit() : product.getMaxOrderLimit());
        product.setStockQuantity(requestDto.getStockQuantity() != null ? requestDto.getStockQuantity() : product.getStockQuantity());

        productRepository.save(product);
    }

    @Transactional
    public void updateProductVisibility(UUID productId, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());
        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        product.setHidden(!product.isHidden());

        productRepository.save(product);
    }

    @Transactional
    public void updateProductDisplayStatus(UUID productId, String displayStatus, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());
        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        ProductDisplayStatusEnum status = ProductDisplayStatusEnum.fromString(displayStatus);
        product.setDisplayStatus(status);

        productRepository.save(product);
    }

    @Transactional
    public void manageProductImage(UUID productId, ProductImageRequestDto requestDto, UserDetailsImpl userDetails) throws IOException {
        Product product = findProductById(productId);

        checkProductIsDeleted(product.isDeleted());
        productOwnershipService.checkProductOwnership(product.getId(), userDetails);

        List<ProductImage> currentImages = product.getProductImages();
        List<ProductImage> newImages = new ArrayList<>();

        if (requestDto.getFiles() != null) {
            newImages = imageService.processProductImages(product, requestDto);
        }

        if (requestDto.getDeletedImageIds() != null && !requestDto.getDeletedImageIds().isEmpty()) {
            currentImages.stream()
                    .filter(image -> requestDto.getDeletedImageIds().contains(image.getId()))
                    .forEach(image -> image.markAsDeleted(userDetails.getUser().getId()));
        }

        if (requestDto.getDeletedImageIds() != null) {
            currentImages = currentImages.stream()
                    .filter(image -> !requestDto.getDeletedImageIds().contains(image.getId()))
                    .toList();
        }

        List<ProductImage> allImages = new ArrayList<>(currentImages);
        allImages.addAll(newImages);

        allImages = imageService.updatePrimaryImage(allImages, requestDto);

        product.setProductImages(allImages);
        productRepository.save(product);
    }

    public ProductResponseDto getProduct(UUID productId, UserDetailsImpl userDetails) {
        Product product = findProductById(productId);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        Set<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean isRoleCustomer = roles.contains("ROLE_CUSTOMER");
        boolean isRoleOwner = roles.contains("ROLE_OWNER");

        if ((isRoleCustomer || isRoleOwner) && product.isDeleted()) {
            throw new ProductIsDeletedException("The product has been deleted.");
        }

        if (isRoleCustomer && product.isHidden()) {
            throw new UnauthorizedAccessException("You do not have permission to view this hidden product.");
        }

        if (isRoleOwner || isRoleCustomer) {
            boolean isProductOwner = productOwnershipService.isProductOwner(product.getId(), userDetails.getUser().getId());
            if (!isProductOwner && !product.getDisplayStatus().equals(ProductDisplayStatusEnum.IN_STOCK)) {
                throw new UnauthorizedAccessException("This product cannot be viewed.");
            }
        }

        if (isRoleOwner && product.isHidden()) {
            productOwnershipService.checkProductOwnership(product.getId(), userDetails);
        }

        List<ProductImageResponseDto> imageResponseDtos = imageService.findActiveImages(productId).stream()
                .map(ProductImageResponseDto::toDto)
                .collect(Collectors.toList());

        return ProductResponseDto.toDto(product, imageResponseDtos);
    }

    private Product findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    }

    private void checkProductIsDeleted(boolean isDeleted) {
        if (isDeleted) {
            throw new ProductIsDeletedException("The product has been deleted.");
        }
    }

    public Page<ProductResponseDto> searchProducts(ProductQueryDto requestDto, UserDetailsImpl userDetails) {
        String role = userDetails.getUser().getRole().getAuthority();

        if ("ROLE_CUSTOMER".equals(role)) {
            if (requestDto.getDisplayStatus() != null && !ProductDisplayStatusEnum.IN_STOCK.name().equals(requestDto.getDisplayStatus())) {
                throw new InvalidDisplayStatusException("Customer는 판매중인 상품만 조회할 수 있습니다.");
            }
        }

        if (requestDto.getStartDate() != null || requestDto.getEndDate() != null) {
            if (!role.equals("ROLE_OWNER") && !role.equals("ROLE_MANAGER") && !role.equals("ROLE_MASTER")) {
                throw new UnauthorizedAccessException("상품의 기간 검색은 Owner, Manager, Master만 수행할 수 있습니다.");
            }
        }

        if (requestDto.getIsHidden() != null) {
            if (!role.equals("ROLE_OWNER") && !role.equals("ROLE_MANAGER") && !role.equals("ROLE_MASTER")) {
                throw new UnauthorizedAccessException("숨겨진 상품은 Owner, Manager, Master만 조회할 수 있습니다.");
            }
        }

        if (requestDto.getIsDeleted() != null) {
            if (!role.equals("ROLE_MANAGER") && !role.equals("ROLE_MASTER")) {
                throw new UnauthorizedAccessException("삭제된 상품은 Manager와 Master만 조회할 수 있습니다.");
            }
        }

        Pageable pageable = PageableUtil.createPageable(requestDto.getPageNumber(), requestDto.getPageSize());
        return productRepository.searchAll(pageable, requestDto, role, userDetails.getUser().getId());
    }

    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    }
}