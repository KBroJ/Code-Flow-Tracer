package com.example.order.service;

import java.util.List;
import java.util.Map;

/**
 * 주문 서비스 인터페이스
 */
public interface OrderService {

    List<OrderVO> selectOrderList(String searchType, String searchKeyword,
                                   String startDate, String endDate, String orderStatus);

    OrderVO selectOrderDetail(String orderId);

    List<OrderItemVO> selectOrderItems(String orderId);

    DeliveryVO selectDeliveryInfo(String orderId);

    String createOrder(OrderVO orderVO);

    void cancelOrder(String orderId, String cancelReason);

    void bulkUpdateOrderStatus(String[] orderIds, String newStatus);

    Map<String, Object> selectOrderStats(String startDate, String endDate, String groupBy);
}
