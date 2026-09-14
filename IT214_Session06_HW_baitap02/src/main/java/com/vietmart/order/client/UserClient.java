package com.vietmart.order.client;

import com.vietmart.order.client.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Interface FeignClient kết nối sang user-service để lấy thông tin người mua.
 * Khai báo đúng theo yêu cầu, không cần implementation class.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserInfo getUserById(@PathVariable("userId") Long userId);
}
