package com.sparta.plate.service.product;

import com.sparta.plate.dto.request.ProductSuggestionQueryDto;
import com.sparta.plate.dto.response.ProductSuggestionResponseDto;
import com.sparta.plate.entity.ProductSuggestionRequest;
import com.sparta.plate.exception.ProductHistoryNotFoundException;
import com.sparta.plate.google.service.GoogleApiService;
import com.sparta.plate.repository.ProductSuggestionRepository;
import com.sparta.plate.util.PageableUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSuggestionService {

    private final GoogleApiService googleApiService;
    private final ProductSuggestionRepository suggestionRepository;

    @Transactional
    public String getProductSuggestion(String requestText, LocalDateTime requestAt) {
        Map<String, Object> responseData = googleApiService.generateContent(requestText);

        String responseText = (String) responseData.get("responseText");
        String statusCode = (String) responseData.get("statusCode");
        String timestamp = (String) responseData.get("timestamp");

        ProductSuggestionRequest suggestionRequest = ProductSuggestionRequest.builder()
                .requestText(requestText)
                .responseText(responseText)
                .requestAt(requestAt)
                .responseAt(LocalDateTime.parse(timestamp))
                .isSuccess("200 OK".equals(statusCode))
                .responseStatus(statusCode)
                .build();

        suggestionRepository.save(suggestionRequest);

        return responseText;
    }

    public Page<ProductSuggestionResponseDto> getSuggestionsHistories(ProductSuggestionQueryDto requestDto) {
        Pageable pageable = PageableUtil.createPageable(requestDto.getPageNumber(), requestDto.getPageSize());
        return suggestionRepository.searchAll(pageable, requestDto);
    }

    @Transactional
    public void deleteProductSuggestion(UUID suggestionId, Long userId) {
        ProductSuggestionRequest suggestionRequest = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ProductHistoryNotFoundException("Product suggestion request not found with ID: " + suggestionId));

        suggestionRequest.markAsDeleted(userId);
        suggestionRepository.save(suggestionRequest);
    }
}
