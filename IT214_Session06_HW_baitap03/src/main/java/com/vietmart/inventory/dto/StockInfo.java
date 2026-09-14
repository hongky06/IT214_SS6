package com.vietmart.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện thông tin tồn kho nhận từ product-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfo {
    private Long productId;
    private Integer availableQuantity;
    private String status;

    /**
     * Dữ liệu fallback trả về khi xảy ra lỗi kết nối hoặc timeout
     */
    public static StockInfo unavailable(Long productId) {
        return StockInfo.builder()
                .productId(productId)
                .availableQuantity(0)
                .status("UNAVAILABLE")
                .build();
    }
}
