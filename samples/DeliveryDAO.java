package com.example.order.dao;

import org.springframework.stereotype.Repository;

/**
 * 배송 DAO
 */
@Repository("deliveryDAO")
public class DeliveryDAO extends EgovAbstractDAO {

    public DeliveryVO selectDelivery(String orderId) {
        return (DeliveryVO) select("deliveryDAO.selectDelivery", orderId);
    }

    public void insertDelivery(DeliveryVO delivery) {
        insert("deliveryDAO.insertDelivery", delivery);
    }

    public void cancelDelivery(String orderId) {
        update("deliveryDAO.cancelDelivery", orderId);
    }

    public void updateDeliveryStatus(String orderId, String status) {
        update("deliveryDAO.updateDeliveryStatus", orderId);
    }
}
