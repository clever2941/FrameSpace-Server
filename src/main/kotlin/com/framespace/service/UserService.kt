package com.framespace.service

import com.baomidou.mybatisplus.extension.service.IService
import com.framespace.common.Result
import com.framespace.dto.AuthRequest
import com.framespace.entity.User

interface UserService : IService<User> {
    fun register(request: AuthRequest): Result<String>
    fun login(request: AuthRequest): Result<String>
}