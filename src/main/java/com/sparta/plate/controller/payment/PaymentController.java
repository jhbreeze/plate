package com.sparta.plate.controller.payment;

import com.sparta.plate.dto.request.PaymentRequestDto;
import com.sparta.plate.dto.response.ApiResponseDto;
import com.sparta.plate.dto.response.PaymentResponseDto;
import com.sparta.plate.entity.Payment;
import com.sparta.plate.repository.PaymentRepository;
import com.sparta.plate.repository.UserRepository;
import com.sparta.plate.security.UserDetailsImpl;
import com.sparta.plate.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // 결제 요청
    @PostMapping("/{orderId}")
    @Operation(summary = "결제 요청", description = "주문 후, 결제를 위해 사용")
    public ApiResponseDto<Map<String, Object>> createPayment(@PathVariable("orderId") UUID orderId,
                                            Long userId,
                                            @RequestBody PaymentRequestDto paymentRequestDto,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        userId = userDetails.getUser().getId();
        PaymentResponseDto paymentResponse = paymentService.createPayment(paymentRequestDto, userId);

        String successMessage = "결제 성공하셨습니다!";
        return ApiResponseDto.success(Map.of(successMessage, paymentResponse));
    }

    // 결제 단건 조회
    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 단건 조회", description = "결제 후, 발급 된 결제 아이디로 결제 조회")
    public ApiResponseDto<Map<String, Object>> getPaymentBypaymentId(@PathVariable("paymentId") UUID paymentId,
                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PaymentResponseDto paymentResponseDto = paymentService.getPaymentBypaymentId(paymentId, userDetails.getUser().getId());
        return ApiResponseDto.success(Map.of("payment", paymentResponseDto));
    }

    // 사용자별 조회
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 결제 조회", description = "로그인 한 사용자의 결제 목록 조회")
    public  ApiResponseDto<Map<String, Object>> findPaymentByUserId(@PathVariable("userId") Long userId,
                                                                                        Pageable pageable,
                                                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Page<PaymentResponseDto> paymentPage = paymentService.findPaymentByUserId(userId, pageable, userDetails);
        return ApiResponseDto.success(Map.of("payments", paymentPage.getContent()));
    }


    // 사용자별 가게 조회 + search(storeName)
    // search : 스토어 이름을 검색해서, 해당되는 스토어만 조회
    @GetMapping("/search/{userId}")
    @Operation(summary = "사용자별 가게 이름 검색 조회", description = "로그인 한 사용자의 결제 목록중, 특정 가게 검색 조회")
    public ApiResponseDto<Page<PaymentResponseDto>> searchPaymentsByUserIdAndStoreName(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "search", required = false) String storeName,
            @RequestParam("page") int page, // 페이지 번호
            @RequestParam("size") int size, // 페이지 사이즈
            @RequestParam("sortBy") String sortBy, // 정렬 기준
            @RequestParam("isAcs") boolean isAcs,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Pageable pageable = createPageable(page, size, sortBy, isAcs);

        // paymentService에서 데이터를 조회
        Page<Payment> paymentPage = paymentService.searchPaymentsByUserIdAndStoreName(userId, storeName, pageable, userDetails);
        Page<PaymentResponseDto> responseDtoPage = paymentPage.map(PaymentResponseDto::new);

        return ApiResponseDto.success(responseDtoPage);
    }

    // 가게별 조회
    // 가게 사장님이, 결제된 내역들을 조회하기 위해!
    @Operation(summary = "가게 결제 내역 조회", description = "가게 사장님이 가게에서 결제된 내역을 조회")
    @GetMapping("/store/{storeId}")
    public ApiResponseDto<Page<PaymentResponseDto>> getPaymentsByStoreId(@PathVariable("storeId") UUID storeId,
                                                         Pageable pageable,
                                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Page<PaymentResponseDto> paymentPage = paymentService.getPaymentsByStoreId(storeId, pageable, userDetails);
        return ApiResponseDto.success(paymentPage);
    }






///////함수///////////////////////////////////////////////////////////////////////

    private Pageable createPageable(int page, int size, String sortBy, boolean isAcs) {
        // 정렬 방향 설정 (오름차순/내림차순)
        Sort.Direction direction = isAcs ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
