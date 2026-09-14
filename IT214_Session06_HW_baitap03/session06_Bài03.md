# BÀI TẬP 3: KHẮC PHỤC CASCADING FAILURE DO THIẾU TIMEOUT TRONG RESTTEMPLATE
**Mã bài toán:** SPRING-CLOUD-S06-EX03 | **Session 06:** Rikkei Education  
**Cấp độ:** Vận dụng chuyên sâu

---

## 1. Liệt kê các lỗi trong đoạn code `StockCheckClient` ban đầu

Đoạn code ban đầu chứa **5 lỗi kỹ thuật nghiêm trọng**:

| STT | Lỗi phát hiện | Mức độ | Phân tích chi tiết |
|---|---|---|---|
| **1** | **Hardcode địa chỉ IP và Port** (`http://192.168.0.12:8082/...`) | **Nghiêm trọng** | Phá vỡ hoàn toàn kiến trúc Service Discovery (Eureka). Khi `product-service` đổi server, đổi IP hoặc scale thành nhiều instance, client sẽ lập tức bị lỗi kết nối hoặc không tận dụng được cân bằng tải. |
| **2** | **RestTemplate thiếu annotation `@LoadBalanced`** | **Nghiêm trọng** | Không thể phân giải Logical Service Name (`http://product-service/...`) qua Client-side Load Balancer. |
| **3** | **RestTemplate KHÔNG cấu hình Timeout (Connect & Read)** | **TỬ HUYỆT** | Mặc định `RestTemplate` không có timeout. Khi `product-service` bị treo, thread gọi request sẽ bị block vô thời hạn. |
| **4** | **Sử dụng Field Injection (`@Autowired`)** | **Trung bình** | Vi phạm nguyên tắc đóng gói, tạo sự phụ thuộc ẩn, gây khó khăn cho Unit Testing. |
| **5** | **Không có xử lý ngoại lệ và Fallback** | **Cao** | Khi downstream service gặp sự cố hoặc timeout, exception bị ném thẳng ra ngoài khiến toàn bộ endpoint của `inventory-service` bị lỗi 500, ảnh hưởng trực tiếp tới client gọi (`order-service`). |

---

## 2. Cơ chế Cascading Failure (Sụp đổ dây chuyền) xảy ra từng bước

1. **Bước 1 — Sự cố khởi nguồn (Root Cause):** `product-service` gặp sự cố (Deadlock database, GC pause kéo dài, hoặc quá tải CPU), khiến thời gian xử lý request kéo dài lên tới 30 giây (hoặc không phản hồi).
2. **Bước 2 — Thread bị chiếm giữ (Thread Blocking):** Mỗi khi `inventory-service` nhận được request kiểm tra tồn kho, nó cấp phát 1 worker thread từ Tomcat Thread Pool để xử lý. Khi gọi `restTemplate.getForObject`, do **không có timeout**, thread này rơi vào trạng thái `WAITING` trên socket read trong 30 giây.
3. **Bước 3 — Cạn kiệt Thread Pool (Thread Pool Exhaustion):**
   - Lưu lượng giờ cao điểm là **50 requests/giây**.
   - Mặc định, Web server nhúng của Spring Boot (Tomcat) có cấu hình `max-threads = 200`.
   - Chỉ sau:
     $$\text{Thời gian cạn kiệt} = \frac{200 \text{ threads}}{50 \text{ req/s}} = \mathbf{4 \text{ giây!}}$$
   - Toàn bộ 200 worker threads của `inventory-service` đều bị chiếm dụng và treo cứng.
4. **Bước 4 — Inventory Service sụp đổ:** `inventory-service` không còn bất kỳ thread nào rảnh rỗi. Mọi request mới gửi đến đều bị từ chối kết nối (`Connection Refused / HTTP 503`). Ngay cả các request không liên quan đến product cũng bị chết theo.
5. **Bước 5 — Lan truyền sang Order Service (Cascading):** `order-service` gọi sang `inventory-service` cũng bị treo do chờ đợi phản hồi $\rightarrow$ các thread của `order-service` cũng bị cạn kiệt tương tự $\rightarrow$ Toàn bộ luồng đặt hàng của khách hàng bị tê liệt hoàn toàn.

---

## 3. Mã nguồn đã sửa đúng chuẩn

Các file mã nguồn Java đã được đóng gói đầy đủ:
- `RestTemplateConfig.java`: Cấu hình `@LoadBalanced RestTemplate` với Connect Timeout = 1s và Read Timeout = 2s.
- `StockCheckClient.java`: Sửa dùng Service ID, tiêm Constructor, bắt `ResourceAccessException` và trả fallback.
- `StockInfo.java`: DTO tồn kho với phương thức fallback an toàn `StockInfo.unavailable(productId)`.
- `StockCheckClientTest.java`: Test tích hợp dùng WireMock kiểm chứng timeout < 3s.

---

## 4. Kiểm chứng bằng Test tích hợp với WireMock

Chạy lệnh `./gradlew test` trong dự án `session06_Bài03`, bài test đã vượt qua thành công (**100% Passed**) với kết quả log thực tế:

```text
16:25:26.544 [Test worker] WARN com.vietmart.inventory.client.StockCheckClient -- 
Lỗi timeout hoặc không thể truy cập product-service khi kiểm tra tồn kho cho productId=101. 
Nguyên nhân: I/O error on GET request for "http://localhost:62628/api/stock/101": Read timed out

====== KẾT QUẢ TEST THỰC NGHIỆM ======
Thời gian phản hồi thực tế: 2107 ms
Trạng thái trả về: UNAVAILABLE
Số lượng tồn kho: 0
======================================
BUILD SUCCESSFUL - 1 test completed, 0 failed.
```

> **Kết luận thực nghiệm:** Mặc dù server giả bị treo 5 giây (5000ms), `RestTemplate` đã chính xác ngắt kết nối sau **2107 ms (< 3 giây)**, giải phóng thread ngay lập tức và trả về fallback an toàn.

---

## 5. Phân tích chuyên sâu & Đề xuất biện pháp bổ sung

### 5.1. Phân tích: Sau khi fix timeout, `order-service` có tiếp tục bị ảnh hưởng nếu request quá tải không?
**CÓ! Hệ thống VẪN CÓ NGUY CƠ BỊ ẢNH HƯỞNG nếu lưu lượng request quá lớn.**
Mặc dù thời gian block đã giảm từ 30s xuống 2s, nhưng mỗi request vẫn giữ 1 worker thread trong **2 giây**. Nếu lưu lượng tăng cao đột biến lên **200 requests/giây**, số lượng thread bị giam trong 2 giây là $200 \times 2 = 400 \text{ threads}$ (vượt quá 200 threads của Tomcat). Do đó, chỉ timeout là chưa đủ.

### 5.2. Đề xuất các biện pháp bổ sung:
1. **Circuit Breaker Pattern (Resilience4j):** Ngắt mạch ngay lập tức (**Fail-Fast trong 0ms**) khi tỷ lệ lỗi vượt ngưỡng, không gửi request qua mạng nữa mà trả fallback ngay, bảo vệ 100% thread pool.
2. **Bulkhead Pattern (Cô lập vách ngăn):** Giới hạn tối đa số thread gọi `product-service` (ví dụ tối đa 20 threads), 180 threads còn lại luôn an toàn phục vụ các chức năng khác.
3. **Rate Limiter (Giới hạn lưu lượng):** Giới hạn số lượng request tối đa tiếp nhận trong 1 giây để chống quá tải.
