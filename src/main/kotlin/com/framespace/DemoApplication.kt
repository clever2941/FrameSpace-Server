package com.framespace

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// 1. 开启 Spring Boot 自动装配（雷达开机）
@SpringBootApplication
// 2. 极其重要：明确告诉大管家去哪里找数据库搬运工（Mapper接口）
@MapperScan("com.framespace.mapper")
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}