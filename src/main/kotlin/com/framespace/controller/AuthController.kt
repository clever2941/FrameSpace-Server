package com.framespace.controller

import com.framespace.common.Result
import com.framespace.dto.AuthRequest
import com.framespace.service.UserService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userService: UserService) {

    /**
     * 注册接口
     * POST /api/auth/register
     */
    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): Result<String> {
        // 简单的基础校验
        if (request.username.isBlank() || request.password.isBlank()) {
            return Result.error(400, "用户名和密码不能为空")
        }
        if (request.password.length < 6) {
            return Result.error(400, "密码长度不能小于 6 位")
        }

        return userService.register(request)
    }

    /**
     * 登录接口
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): Result<String> {
        if (request.username.isBlank() || request.password.isBlank()) {
            return Result.error(400, "用户名和密码不能为空")
        }

        return userService.login(request)
    }
}