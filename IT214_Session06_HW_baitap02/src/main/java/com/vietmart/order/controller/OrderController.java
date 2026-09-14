package com.vietmart.order.controller;

import com.vietmart.order.client.ProductClient;
import com.vietmart.order.client.UserClient;
import com.vietmart.order.client.dto.ProductInfo;
import com.vietmart.order.client.dto.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller minh họa cách sử dụng ProductClient và UserClient trong order-service.
 */
@RestController
@RequestMapping("/api/orders/demo")
@RequiredArgsConstructor
public class OrderController {

    private final ProductClient productClient;
    private final UserClient userClient;

    /**
     * Demo lấy chi tiết 1 sản phẩm qua FeignClient (kèm fallback nếu product-service tắt)
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductInfo> getProductById(@PathVariable Long id) {
        ProductInfo product = productClient.getById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Demo lấy toàn bộ sản phẩm qua FeignClient (trả về empty list nếu fallback)
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductInfo>> getAllProducts() {
        List<ProductInfo> products = productClient.getAll();
        return ResponseEntity.ok(products);
    }

    /**
     * Demo lấy thông tin người mua qua FeignClient
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable Long userId) {
        UserInfo user = userClient.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
