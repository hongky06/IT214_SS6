# BÀI TẬP 2: CHUYỂN ĐỔI PRODUCTSERVICECLIENTRT SANG FEIGNCLIENT
**Mã bài toán:** SPRING-CLOUD-S06-EX02 | **Session 06:** Rikkei Education  
**Cấp độ:** Vận dụng cơ bản / Chuyên sâu

---

## 1. Mục tiêu và Bối cảnh chuyển đổi

Trong kiến trúc Microservice, giao tiếp đồng bộ qua REST giữa các service có 2 cách tiếp cận chính:
1. **Imperative REST Client (`RestTemplate`):** Lập trình viên phải tự viết mã gọi thủ công, tự xây dựng URL dạng String, truyền tham số, cấu hình load balancing và tự viết các khối `try - catch` để xử lý ngoại lệ.
2. **Declarative REST Client (`Spring Cloud OpenFeign`):** Lập trình viên chỉ cần định nghĩa một **Java Interface** kèm theo các annotation của Spring MVC (`@GetMapping`, `@PathVariable`). Spring Cloud sẽ tự động sinh proxy runtime, tích hợp sẵn Service Discovery (Eureka), Load Balancer và Circuit Breaker/Fallback.

Bài tập yêu cầu chuyển đổi lớp `ProductServiceClientRT` sang `ProductClient` (FeignClient), bổ sung `UserClient` và xây dựng `ProductClientFallbackFactory` để xử lý ngoại lệ an toàn.

---

## 2. Danh sách các file mã nguồn đã tách riêng

Mã nguồn đã được tách thành các file độc lập chuẩn Java:
- 👉 [**`ProductClient.java`**](file:///e:/IT214_BTVN/ProductClient.java): Interface FeignClient thay thế `ProductServiceClientRT`, kết nối tới `product-service`.
- 👉 [**`UserClient.java`**](file:///e:/IT214_BTVN/UserClient.java): Interface FeignClient kết nối tới `user-service` lấy thông tin người mua.
- 👉 [**`ProductClientFallbackFactory.java`**](file:///e:/IT214_BTVN/ProductClientFallbackFactory.java): Lớp FallbackFactory bắt exception, ghi log chi tiết và trả dữ liệu dự phòng.
- 👉 [**`ProductInfo.java`**](file:///e:/IT214_BTVN/ProductInfo.java): DTO dữ liệu sản phẩm kèm phương thức `fallback(Long id)`.
- 👉 [**`UserInfo.java`**](file:///e:/IT214_BTVN/UserInfo.java): DTO dữ liệu người dùng.

---

## 3. Chi tiết mã nguồn hiện thực

### 3.1. Interface `ProductClient.java`
Thay thế hoàn toàn lớp `ProductServiceClientRT`, không cần class implementation:

```java
package com.vietmart.order.client;

import com.vietmart.order.client.dto.ProductInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductInfo getById(@PathVariable("id") Long id);

    @GetMapping("/api/products")
    List<ProductInfo> getAll();
}
```

---

### 3.2. Interface `UserClient.java`
Khai báo Feign Client lấy thông tin người mua từ `user-service`:

```java
package com.vietmart.order.client;

import com.vietmart.order.client.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserInfo getUserById(@PathVariable("userId") Long userId);
}
```

---

### 3.3. Lớp `ProductClientFallbackFactory.java`
Cài đặt `FallbackFactory<ProductClient>` để ghi log nguyên nhân gốc (`Throwable cause`) và trả dữ liệu dự phòng:

```java
package com.vietmart.order.client;

import com.vietmart.order.client.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);

    @Override
    public ProductClient create(Throwable cause) {
        // Ghi log chi tiết lỗi kèm exception trace để phục vụ giám sát / debug
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
```

---

## 4. Báo cáo so sánh chi tiết: `RestTemplate` vs `FeignClient`

### 4.1. Bảng đối chiếu số dòng code và độ phức tạp

| Tiêu chí | `ProductServiceClientRT` (RestTemplate) | `ProductClient` (FeignClient) |
|---|---|---|
| **Dạng định nghĩa** | Class cụ thể (`@Component class`) | Chỉ là Interface (`public interface`) |
| **Số dòng code nghiệp vụ gọi API** | ~35 - 50 dòng (tính cả `getById` và `getAll`) | **Khoảng 6 dòng** (mỗi endpoint chỉ 2 dòng) |
| **Boilerplate Code** | Rất nhiều: Tiêm bean, viết URL chuỗi tĩnh, map kiểu `.class`, cấu hình tham số | **Gần như bằng 0**: Dùng lại các annotation Spring MVC quen thuộc |
| **Quản lý URL & Endpoint** | Dễ gõ sai URL chuỗi (`"http://product-service/api/..."`) | Rõ ràng, khai báo tường minh qua `@GetMapping` |
| **Tách biệt xử lý Fallback** | Nằm lẫn lộn trong các khối `try - catch` trong từng method gọi | Tách biệt hoàn toàn vào `ProductClientFallbackFactory` |
| **Khả năng tiếp cận ngoại lệ gốc** | Tự bắt trong từng method | `FallbackFactory` cung cấp trực tiếp `Throwable cause` |

---

### 4.2. Tại sao nên dùng `FallbackFactory` thay vì `fallback` thông thường?

Trong Spring Cloud OpenFeign có 2 cơ chế fallback:
1. `fallback = ProductClientFallback.class`: Lớp fallback chỉ có thể trả về dữ liệu rỗng/mặc định mà **hoàn toàn không biết lý do vì sao request bị lỗi**.
2. `fallbackFactory = ProductClientFallbackFactory.class` (**Khuyên dùng**): Phương thức `create(Throwable cause)` truyền vào ngoại lệ gốc giúp:
   - **Ghi log chính xác lỗi:** Biết được lỗi là do `SocketTimeoutException` (server phản hồi chậm), `ConnectException` (service bị sập), `HttpServerErrorException` (lỗi 500 từ server) hay do `CircuitBreakerOpenException`.
   - **Xử lý phân nhánh linh hoạt:** Có thể kiểm tra kiểu ngoại lệ để quyết định trả về dữ liệu cache, rethrow exception phù hợp hoặc kích hoạt cảnh báo tới hệ thống giám sát.

---

### 4.3. Lập luận: Khi nào nên sử dụng mỗi phương pháp?

#### Khi nào NÊN DÙNG OpenFeign (Declarative Client)?
- **Giao tiếp nội bộ giữa các microservice (Inter-service communication):** Các service trong cùng hệ sinh thái (đăng ký qua Eureka/Consul/K8s).
- **Mã nguồn ngắn gọn, dễ đọc, dễ bảo trì:** Phù hợp với chuẩn phát triển chung của team, tương tự như phong cách Spring Data JPA.
- **Tích hợp sâu hệ sinh thái Spring Cloud:** Dễ dàng gắn kèm Spring Cloud LoadBalancer, Resilience4j Circuit Breaker, OpenTelemetry/Micrometer tracing mà không cần viết thêm code can thiệp.

#### Khi nào NÊN DÙNG RestTemplate / WebClient (Imperative Client)?
- **Gọi sang các API bên thứ 3 (Third-party External APIs):** Các hệ thống bên ngoài không nằm trong Service Discovery, có URL động hoặc cấu trúc xác thực đặc thù (OAuth2 phức tạp, chữ ký điện tử HMAC).
- **Tải file nhị phân dung lượng lớn (Large file download / streaming):** Cần kiểm soát chặt chẽ luồng InputStream và bộ đệm bộ nhớ.
- **Tùy biến sâu ở tầng HTTP Transport:** Khi cần can thiệp vào tầng socket thấp, cấu hình SSL/TLS handshake riêng cho từng request riêng biệt.

---

## 5. Hướng dẫn cấu hình kích hoạt FeignClient trong ứng dụng

### 5.1. Kích hoạt trong Main Class (`OrderServiceApplication.java`):
```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.vietmart.order.client")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 5.2. Kích hoạt tính năng Fallback trong `application.yml`:
Để FeignClient kích hoạt được `fallbackFactory`, cần bật cờ circuitbreaker trong cấu hình:
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

---

## 6. Kết quả nghiệm thu theo yêu cầu đề bài

✅ **Yêu cầu 1:** `ProductClient` được tạo dưới dạng Interface với `@FeignClient(name="product-service")`, khai báo đầy đủ 2 phương thức `getById(Long id)` và `getAll()`.  
✅ **Yêu cầu 2:** `UserClient` được tạo dưới dạng Interface với `@FeignClient(name="user-service")`, khai báo `getUserById(Long userId)` trả về `UserInfo`.  
✅ **Yêu cầu 3:** `ProductClientFallbackFactory` cài đặt đầy đủ `FallbackFactory<ProductClient>`, log lỗi qua `log.error`, trả về `ProductInfo.fallback(id)` cho `getById()` và `Collections.emptyList()` cho `getAll()`.  
✅ **Yêu cầu 4:** Báo cáo so sánh số dòng code, cấu trúc và lập luận rõ ràng về phạm vi ứng dụng của từng phương pháp.
