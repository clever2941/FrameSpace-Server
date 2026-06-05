package com.framespace.service.impl

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.framespace.common.Result
import com.framespace.dto.AuthRequest
import com.framespace.entity.User
import com.framespace.mapper.UserMapper
import com.framespace.service.UserService
import com.framespace.utils.JwtUtils
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserServiceImpl : ServiceImpl<UserMapper, User>(), UserService {

    override fun register(request: AuthRequest): Result<String> {
        // 1. 检查用户名是否已存在
        val existingUser = this.ktQuery().eq(User::username, request.username).one()
        if (existingUser != null) {
            return Result.error(400, "该用户名已被注册")
        }

        // 2. 使用 BCrypt 对密码进行加密加盐
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

        // 3. 构建新用户并保存 (完美匹配你的 password_hash 字段)
        val newUser = User(
            username = request.username,
            passwordHash = hashedPassword,
            createdAt = LocalDateTime.now()
        )

        val isSuccess = this.save(newUser)
        return if (isSuccess) {
            Result.success("注册成功")
        } else {
            Result.error(500, "服务器异常，注册失败")
        }
    }

    override fun login(request: AuthRequest): Result<String> {
        // 1. 根据用户名查找用户
        val user = this.ktQuery().eq(User::username, request.username).one()
            ?: return Result.error(401, "用户名或密码错误")

        // 2. 校验密码 (比对你数据库里的 password_hash)
        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            return Result.error(401, "用户名或密码错误")
        }

        // 3. 密码正确，生成 JWT Token 返回
        val token = JwtUtils.generateToken(user.id!!, user.username!!)
        return Result.success(token)
    }
}