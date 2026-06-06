plugins {
    id("org.springframework.boot") version "3.2.6"
    // 👇 就是下面这个 1.1.5 版本，专门修复了你截图里的那个恶心报错！
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
}

group = "com.framespace"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    // 继续使用阿里云镜像，保证下载不中断
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/spring") }
    mavenCentral()
}

dependencies {
    // 基础 Web 与数据库连接支持
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Kotlin 扩展
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // 🔥 极其关键：这是专门为 Spring Boot 3 定制的 MyBatis-Plus！加上它就不会报那个错了！
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.5")

    // 现代版本的 MySQL 驱动
    runtimeOnly("com.mysql:mysql-connector-j")

    // 密码加密与 JWT 工具
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
}

// 修复了你截图里的 kotlinOptions 警告，使用最新的语法标准
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}