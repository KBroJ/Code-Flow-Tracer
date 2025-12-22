package com.example.order.dao;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

/**
 * 주문 DAO
 */
@Repository("orderDAO")
public class OrderDAO extends EgovAbstractDAO {

    public List<OrderVO> selectOrderList(Map<String, Object> params) {
        return selectList("orderDAO.selectOrderList", params);
    }

    public OrderVO selectOrder(String orderId) {
        return (OrderVO) select("orderDAO.selectOrder", orderId);
    }

    public CustomerVO selectCustomerInfo(String customerId) {
        return (CustomerVO) select("orderDAO.selectCustomerInfo", customerId);
    }

    public List<OrderItemVO> selectOrderItems(String orderId) {
        return selectList("orderDAO.selectOrderItems", orderId);
    }

    public String insertOrder(OrderVO orderVO) {
        return (String) insert("orderDAO.insertOrder", orderVO);
    }

    public void insertOrderItem(OrderItemVO item) {
        insert("orderDAO.insertOrderItem", item);
    }

    public void updateOrderStatus(String orderId, String status, String reason) {
        Map<String, Object> params = Map.of("orderId", orderId, "status", status, "reason", reason);
        update("orderDAO.updateOrderStatus", params);
    }

    public void bulkUpdateOrderStatus(List<String> orderIds, String status) {
        Map<String, Object> params = Map.of("orderIds", orderIds, "status", status);
        update("orderDAO.bulkUpdateOrderStatus", params);
    }

    public Map<String, Object> selectOrderSummary(Map<String, Object> params) {
        return (Map<String, Object>) select("orderDAO.selectOrderSummary", params);
    }

    public List<Map<String, Object>> selectDailyStats(Map<String, Object> params) {
        return selectList("orderDAO.selectDailyStats", params);
    }
}
