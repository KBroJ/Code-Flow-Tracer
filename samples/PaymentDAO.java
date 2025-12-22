package com.example.order.dao;

import org.springframework.stereotype.Repository;

/**
 * 결제 DAO
 */
@Repository("paymentDAO")
public class PaymentDAO extends EgovAbstractDAO {

    public void insertPayment(PaymentVO payment) {
        insert("paymentDAO.insertPayment", payment);
    }

    public void cancelPayment(String orderId) {
        update("paymentDAO.cancelPayment", orderId);
    }

    public PaymentVO selectPayment(String orderId) {
        return (PaymentVO) select("paymentDAO.selectPayment", orderId);
    }
}
