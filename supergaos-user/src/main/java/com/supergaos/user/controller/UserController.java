package com.supergaos.user.controller;

import com.supergaos.common.result.Result;
import com.supergaos.user.dto.LoginDTO;
import com.supergaos.user.dto.RegisterDTO;
import com.supergaos.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
}
