package com.example.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import javax.annotation.Resource;
import java.util.Map;

/**
 * 주문 관리 컨트롤러
 * - 복잡한 비즈니스 로직 (여러 Service/DAO 호출)
 * - 다양한 파라미터 처리
 */
@Controller
@RequestMapping("/order")
public class OrderController {

    @Resource(name = "orderService")
    private OrderService orderService;

    /**
     * 주문 목록 조회 (복잡한 검색 조건)
     */
    @GetMapping("/list.do")
    public String selectOrderList(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderStatus,
            Model model) {

        model.addAttribute("orderList", orderService.selectOrderList(searchType, searchKeyword, startDate, endDate, orderStatus));
        return "order/orderList";
    }

    /**
     * 주문 상세 조회 (주문 + 상품 + 배송 정보)
     */
    @GetMapping("/detail.do")
    public String selectOrderDetail(@RequestParam String orderId, Model model) {
        model.addAttribute("order", orderService.selectOrderDetail(orderId));
        model.addAttribute("orderItems", orderService.selectOrderItems(orderId));
        model.addAttribute("delivery", orderService.selectDeliveryInfo(orderId));
        return "order/orderDetail";
    }

    /**
     * 주문 생성 (재고 확인 → 주문 생성 → 재고 차감 → 결제)
     */
    @PostMapping("/create.do")
    public String createOrder(@RequestBody OrderVO orderVO, Model model) {
        String orderId = orderService.createOrder(orderVO);
        model.addAttribute("orderId", orderId);
        return "redirect:/order/complete.do?orderId=" + orderId;
    }

    /**
     * 주문 취소 (결제 취소 → 재고 복구 → 주문 상태 변경)
     */
    @PostMapping("/cancel.do")
    public String cancelOrder(@RequestParam String orderId, @RequestParam String cancelReason) {
        orderService.cancelOrder(orderId, cancelReason);
        return "redirect:/order/list.do";
    }

    /**
     * 대량 주문 상태 변경
     */
    @PostMapping("/bulkUpdateStatus.do")
    public String bulkUpdateOrderStatus(@RequestParam("orderIds") String[] orderIds,
                                        @RequestParam String newStatus) {
        orderService.bulkUpdateOrderStatus(orderIds, newStatus);
        return "redirect:/order/list.do";
    }

    /**
     * 주문 통계 조회
     */
    @GetMapping("/stats.do")
    public String selectOrderStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String groupBy,
            Model model) {
        model.addAttribute("stats", orderService.selectOrderStats(startDate, endDate, groupBy));
        return "order/orderStats";
    }
}
