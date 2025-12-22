package com.example.order.service.impl;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * 주문 서비스 구현체
 * - 여러 DAO 호출
 * - 복잡한 트랜잭션 처리
 */
@Service("orderService")
public class OrderServiceImpl implements OrderService {

    @Resource(name = "orderDAO")
    private OrderDAO orderDAO;

    @Resource(name = "productDAO")
    private ProductDAO productDAO;

    @Resource(name = "stockDAO")
    private StockDAO stockDAO;

    @Resource(name = "paymentDAO")
    private PaymentDAO paymentDAO;

    @Resource(name = "deliveryDAO")
    private DeliveryDAO deliveryDAO;

    /**
     * 주문 목록 조회 (동적 검색)
     */
    @Override
    public List<OrderVO> selectOrderList(String searchType, String searchKeyword,
                                          String startDate, String endDate, String orderStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("searchType", searchType);
        params.put("searchKeyword", searchKeyword);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("orderStatus", orderStatus);

        return orderDAO.selectOrderList(params);
    }

    /**
     * 주문 상세 조회
     */
    @Override
    public OrderVO selectOrderDetail(String orderId) {
        OrderVO order = orderDAO.selectOrder(orderId);

        // 주문자 정보 조회
        if (order != null) {
            order.setCustomerInfo(orderDAO.selectCustomerInfo(order.getCustomerId()));
        }

        return order;
    }

    /**
     * 주문 상품 목록 조회 (상품 정보 포함)
     */
    @Override
    public List<OrderItemVO> selectOrderItems(String orderId) {
        List<OrderItemVO> items = orderDAO.selectOrderItems(orderId);

        // 각 상품의 상세 정보 조회
        for (OrderItemVO item : items) {
            item.setProductInfo(productDAO.selectProduct(item.getProductId()));
            item.setStockInfo(stockDAO.selectStock(item.getProductId()));
        }

        return items;
    }

    /**
     * 배송 정보 조회
     */
    @Override
    public DeliveryVO selectDeliveryInfo(String orderId) {
        return deliveryDAO.selectDelivery(orderId);
    }

    /**
     * 주문 생성 (복잡한 트랜잭션)
     * 1. 상품 유효성 검증
     * 2. 재고 확인
     * 3. 주문 생성
     * 4. 주문 상품 생성
     * 5. 재고 차감
     * 6. 결제 처리
     * 7. 배송 정보 생성
     */
    @Override
    public String createOrder(OrderVO orderVO) {
        // 1. 상품 유효성 검증
        for (OrderItemVO item : orderVO.getItems()) {
            ProductVO product = productDAO.selectProduct(item.getProductId());
            if (product == null || !"Y".equals(product.getSaleYn())) {
                throw new RuntimeException("Invalid product: " + item.getProductId());
            }
        }

        // 2. 재고 확인
        for (OrderItemVO item : orderVO.getItems()) {
            StockVO stock = stockDAO.selectStock(item.getProductId());
            if (stock.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock: " + item.getProductId());
            }
        }

        // 3. 주문 생성
        String orderId = orderDAO.insertOrder(orderVO);

        // 4. 주문 상품 생성
        for (OrderItemVO item : orderVO.getItems()) {
            item.setOrderId(orderId);
            orderDAO.insertOrderItem(item);
        }

        // 5. 재고 차감
        for (OrderItemVO item : orderVO.getItems()) {
            stockDAO.decreaseStock(item.getProductId(), item.getQuantity());
            stockDAO.insertStockHistory(item.getProductId(), "OUT", item.getQuantity(), orderId);
        }

        // 6. 결제 처리
        PaymentVO payment = new PaymentVO();
        payment.setOrderId(orderId);
        payment.setAmount(orderVO.getTotalAmount());
        paymentDAO.insertPayment(payment);

        // 7. 배송 정보 생성
        DeliveryVO delivery = new DeliveryVO();
        delivery.setOrderId(orderId);
        delivery.setAddress(orderVO.getDeliveryAddress());
        deliveryDAO.insertDelivery(delivery);

        return orderId;
    }

    /**
     * 주문 취소 (복잡한 롤백)
     * 1. 결제 취소
     * 2. 재고 복구
     * 3. 주문 상태 변경
     * 4. 배송 취소
     */
    @Override
    public void cancelOrder(String orderId, String cancelReason) {
        // 1. 결제 취소
        paymentDAO.cancelPayment(orderId);

        // 2. 재고 복구
        List<OrderItemVO> items = orderDAO.selectOrderItems(orderId);
        for (OrderItemVO item : items) {
            stockDAO.increaseStock(item.getProductId(), item.getQuantity());
            stockDAO.insertStockHistory(item.getProductId(), "IN", item.getQuantity(), orderId);
        }

        // 3. 주문 상태 변경
        orderDAO.updateOrderStatus(orderId, "CANCELLED", cancelReason);

        // 4. 배송 취소
        deliveryDAO.cancelDelivery(orderId);
    }

    /**
     * 대량 주문 상태 변경
     */
    @Override
    public void bulkUpdateOrderStatus(String[] orderIds, String newStatus) {
        orderDAO.bulkUpdateOrderStatus(Arrays.asList(orderIds), newStatus);
    }

    /**
     * 주문 통계 조회
     */
    @Override
    public Map<String, Object> selectOrderStats(String startDate, String endDate, String groupBy) {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("groupBy", groupBy);

        Map<String, Object> stats = new HashMap<>();
        stats.put("summary", orderDAO.selectOrderSummary(params));
        stats.put("daily", orderDAO.selectDailyStats(params));
        stats.put("topProducts", productDAO.selectTopProducts(params));

        return stats;
    }
}
