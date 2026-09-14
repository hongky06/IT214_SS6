package com.vietmart.inventory.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.vietmart.inventory.dto.StockInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test tích hợp sử dụng WireMock:
 * Mô phỏng server giả delay 5 giây (5000ms).
 * Xác nhận StockCheckClient ngắt timeout và trả về fallback trong vòng 3 giây (< 3000ms),
 * chứ KHÔNG PHẢI đợi sau 5 giây.
 */
class StockCheckClientTest {

    private WireMockServer wireMockServer;
    private StockCheckClient stockCheckClient;

    @BeforeEach
    void setUp() {
        // 1. Khởi động WireMockServer trên cổng động
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // 2. Khởi tạo RestTemplate cấu hình đúng: connectTimeout = 1s, readTimeout = 2s
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(1))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();

        // 3. Khởi tạo StockCheckClient trỏ tới WireMockServer
        stockCheckClient = new StockCheckClient(restTemplate, "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Khi server delay 5s -> RestTemplate ngắt timeout sau 2s và trả về Fallback trong < 3s")
    void checkStock_ShouldReturnFallbackWithin3Seconds_WhenServerDelays5Seconds() {
        // Arrange: Cấu hình WireMock giả lập product-service bị treo, phản hồi sau 5000ms (5 giây)
        wireMockServer.stubFor(get(urlEqualTo("/api/stock/101"))
                .willReturn(aResponse()
                        .withFixedDelay(5000) // Delay 5 giây
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"productId\": 101, \"availableQuantity\": 50, \"status\": \"AVAILABLE\"}")));

        // Act: Gọi checkStock và bấm giờ thực thi
        long startTime = System.currentTimeMillis();
        StockInfo result = stockCheckClient.checkStock(101L);
        long durationMs = System.currentTimeMillis() - startTime;

        System.out.println("====== KẾT QUẢ TEST THỰC NGHIỆM ======");
        System.out.println("Thời gian phản hồi thực tế: " + durationMs + " ms");
        System.out.println("Trạng thái trả về: " + result.getStatus());
        System.out.println("Số lượng tồn kho: " + result.getAvailableQuantity());
        System.out.println("======================================");

        // Assert:
        // 1. Dữ liệu fallback an toàn phải được trả về
        assertNotNull(result, "Kết quả không được null");
        assertEquals("UNAVAILABLE", result.getStatus(), "Trạng thái phải là UNAVAILABLE");
        assertEquals(0, result.getAvailableQuantity(), "Số lượng tồn kho fallback phải là 0");
        assertEquals(101L, result.getProductId(), "ProductId phải khớp với ID yêu cầu");

        // 2. Thời gian phản hồi phải dưới 3 giây (< 3000ms) thay vì bị treo 5 giây (5000ms)
        assertTrue(durationMs < 3000,
                "Thời gian phản hồi phải dưới 3000ms (Thực tế: " + durationMs + "ms)");

        // 3. Thời gian phản hồi tối thiểu phải đạt ngưỡng readTimeout (khoảng 2000ms)
        assertTrue(durationMs >= 1900,
                "Thời gian phản hồi phải tối thiểu đạt ngưỡng readTimeout 2000ms (Thực tế: " + durationMs + "ms)");
    }
}
