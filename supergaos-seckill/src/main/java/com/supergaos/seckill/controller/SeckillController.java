package com.supergaos.seckill.controller;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.common.result.Result;
import com.supergaos.seckill.entity.SeckillActivity;
import com.supergaos.seckill.entity.SeckillOrder;
import com.supergaos.seckill.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillService seckillService;
    public SeckillController(SeckillService seckillService) { this.seckillService = seckillService; }

    @GetMapping("/list")
    public Result<List<SeckillActivity>> list() { return Result.success(seckillService.listActivities()); }

    @GetMapping("/{id}")
    public Result<SeckillActivity> detail(@PathVariable Long id) { return Result.success(seckillService.getActivity(id)); }

    @PostMapping("/{id}/grab")
    public Result<String> grab(@PathVariable Long id, HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null) throw new BusinessException(1001, "未登录");
        return Result.success(seckillService.grab(id, Long.parseLong(userIdStr)));
    }

    @GetMapping("/orders")
    public Result<List<SeckillOrder>> orders(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null) throw new BusinessException(1001, "未登录");
        return Result.success(seckillService.getOrders(Long.parseLong(userIdStr)));
    }
}
