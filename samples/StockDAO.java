package com.example.order.dao;

import org.springframework.stereotype.Repository;
import java.util.Map;

/**
 * 재고 DAO
 */
@Repository("stockDAO")
public class StockDAO extends EgovAbstractDAO {

    public StockVO selectStock(String productId) {
        return (StockVO) select("stockDAO.selectStock", productId);
    }

    public void decreaseStock(String productId, int quantity) {
        Map<String, Object> params = Map.of("productId", productId, "quantity", quantity);
        update("stockDAO.decreaseStock", params);
    }

    public void increaseStock(String productId, int quantity) {
        Map<String, Object> params = Map.of("productId", productId, "quantity", quantity);
        update("stockDAO.increaseStock", params);
    }

    public void insertStockHistory(String productId, String type, int quantity, String refId) {
        Map<String, Object> params = Map.of(
            "productId", productId,
            "type", type,
            "quantity", quantity,
            "refId", refId
        );
        insert("stockDAO.insertStockHistory", params);
    }
}
