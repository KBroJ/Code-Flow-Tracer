package com.example.order.dao;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

/**
 * 상품 DAO
 */
@Repository("productDAO")
public class ProductDAO extends EgovAbstractDAO {

    public ProductVO selectProduct(String productId) {
        return (ProductVO) select("productDAO.selectProduct", productId);
    }

    public List<ProductVO> selectProductList(Map<String, Object> params) {
        return selectList("productDAO.selectProductList", params);
    }

    public List<Map<String, Object>> selectTopProducts(Map<String, Object> params) {
        return selectList("productDAO.selectTopProducts", params);
    }

    public void updateProductSaleYn(String productId, String saleYn) {
        Map<String, Object> params = Map.of("productId", productId, "saleYn", saleYn);
        update("productDAO.updateProductSaleYn", params);
    }
}
