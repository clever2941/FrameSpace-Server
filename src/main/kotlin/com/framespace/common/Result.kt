package com.framespace.common

/**
 * 统一后端返回结果包装类
 * @param code 状态码（例如：200 表示成功，404 表示未找到）
 * @param msg  提示信息
 * @param data 核心业务数据
 */
data class Result<T>(
    val code: Int,
    val msg: String,
    val data: T?
) {
    companion object {
        // 快捷返回成功结果的方法
        fun <T> success(data: T): Result<T> {
            return Result(200, "success", data)
        }

        // 快捷返回错误结果的方法
        fun <T> error(code: Int, msg: String): Result<T> {
            return Result(code, msg, null)
        }
    }
}