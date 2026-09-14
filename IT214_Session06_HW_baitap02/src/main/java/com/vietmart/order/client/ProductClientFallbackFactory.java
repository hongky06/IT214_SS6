package com.vietmart.order.client;

import com.vietmart.order.client.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * FallbackFactory cho ProductClient.
 * Bắt và ghi log chi tiết ngoại lệ (Throwable cause) gây ra sự cố
 * khi gọi sang product-service, đồng thời trả về dữ liệu dự phòng an toàn.
 */
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        // Ghi log chi tiết lỗi khi kết nối tới product-service thất bại
        log.error("Lỗi khi gọi sang product-service qua FeignClient. Nguyên nhân: {}", cause.getMessage(), cause);

        return new ProductClient() {
            @Override
            public ProductInfo getById(Long id) {
                log.warn("Kích hoạt Fallback cho getById(id={}). Trả về đối tượng ProductInfo dự phòng.", id);
                return ProductInfo.fallback(id);
            }

            @Override
            public List<ProductInfo> getAll() {
                log.warn("Kích hoạt Fallback cho getAll(). Trả về danh sách rỗng.");
                return Collections.emptyList();
            }
        };
    }
}
