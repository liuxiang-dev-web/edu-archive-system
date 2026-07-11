package com.edu.archive.controller;


// 【健康检查控制器】提供 /api/health 接口，返回服务运行状态。
import com.edu.archive.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public ApiResponse<?> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "time", LocalDateTime.now().toString()
        ));
    }
}
