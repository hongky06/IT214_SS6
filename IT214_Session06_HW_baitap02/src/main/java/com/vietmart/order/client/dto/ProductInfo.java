package com.vietmart.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO đại diện thông tin sản phẩm nhận từ product-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInfo {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;

    /**
     * Dữ liệu dự phòng an toàn khi product-service gặp sự cố hoặc ngắt mạch
     */
    public static ProductInfo fallback(Long id) {
        return ProductInfo.builder()
                .id(id)
                .name("Sản phẩm tạm thời không khả dụng (Fallback)")
                .price(BigDecimal.ZERO)
                .stockQuantity(0)
                .status("UNAVAILABLE")
                .build();
    }
}
