package com.vietmart.inventory.client;

import com.vietmart.inventory.dto.StockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Client kiểm tra tồn kho kết nối sang product-service.
 * Đã sửa toàn bộ lỗi:
 * 1. Bỏ IP hardcode -> Dùng Service ID (http://product-service/api/stock/{pid}).
 * 2. RestTemplate tiêm qua Constructor có @LoadBalanced và timeout (Connect: 1s, Read: 2s).
 * 3. Bắt ngoại lệ ResourceAccessException (xảy ra khi timeout hoặc không kết nối được)
 *    và trả về dữ liệu fallback StockInfo.unavailable(productId).
 */
@Component
public class StockCheckClient {

    private static final Logger log = LoggerFactory.getLogger(StockCheckClient.class);

    private final RestTemplate restTemplate;
    private String baseUrl = "http://product-service";

    @Autowired
    public StockCheckClient(@LoadBalanced RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Constructor hỗ trợ truyền baseUrl tùy chỉnh (dùng cho integration test)
     */
    public StockCheckClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public StockInfo checkStock(Long productId) {
        try {
            return restTemplate.getForObject(
                    baseUrl + "/api/stock/{pid}",
                    StockInfo.class,
                    productId
            );
        } catch (ResourceAccessException e) {
            // Xảy ra khi connectTimeout (quá 1s) hoặc readTimeout (quá 2s) hoặc server mất kết nối
            log.warn("Lỗi timeout hoặc không thể truy cập product-service khi kiểm tra tồn kho cho productId={}. Nguyên nhân: {}",
                    productId, e.getMessage());
            return StockInfo.unavailable(productId);
        } catch (Exception e) {
            log.error("Lỗi không mong muốn khi kiểm tra tồn kho cho productId={}. Nguyên nhân: {}", productId, e.getMessage(), e);
            return StockInfo.unavailable(productId);
        }
    }
}
