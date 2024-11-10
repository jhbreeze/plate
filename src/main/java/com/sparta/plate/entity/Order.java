package com.sparta.plate.entity;

import com.sparta.plate.dto.request.OrderRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_order")  // 테이블 이름 설정
@Getter
@Setter
@NoArgsConstructor
public class Order extends Timestamped{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)  // 외래 키 설정 (user_id)
    private User user;  // 주문한 사용자 (User 엔티티와 연관)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", referencedColumnName = "id", nullable = false)  // 외래 키 설정 (store_id)
    private Store store;  // 주문이 발생한 상점 (Store 엔티티와 연관)

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderTypeEnum orderTypeEnum;  // 주문 유형 (ENUM)

    @Column(name = "order_price", nullable = false, precision = 10, scale = 0)
    private Long orderPrice;  // 주문 금액 (NUMERIC(10,0))

    @Column(name = "is_canceled", nullable = false)
    private Boolean isCanceled;  // 주문 취소 여부 (BOOLEAN)

    @Column(name = "order_address", nullable = false, length = 225)
    private String orderAddress;  // 배송지 주소 (VARCHAR(225))

    @Column(name = "order_request", length = 500)
    private String orderRequest;  // 주문 요청 사항 (VARCHAR(500))

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatusEnum orderStatusEnum;  // 주문 상태 (ENUM)

    @OneToMany(mappedBy = "order")  // Order 엔티티에서 'product' 필드를 관리
    private List<OrderProduct> orderProductList = new ArrayList<>();


    public Order(OrderRequestDto requestDto,User user, Store store, List<OrderProduct> orderProductList) {
        this.user = user;
        this.store = store;
        this.orderTypeEnum = requestDto.getOrderType();
        this.orderPrice = requestDto.getOrderPrice();
        this.isCanceled = requestDto.getIsCanceled();
        this.orderAddress = requestDto.getOrderAddress();
        this.orderRequest = requestDto.getOrderRequest();
        this.orderStatusEnum = requestDto.getOrderStatus();
        this.orderProductList = orderProductList;
    }

}