# ==========================================
# 第一阶段：编译打包阶段 (取名叫 builder)
# ==========================================
# 使用带有 Maven 的环境来编译代码
FROM maven:3.8.5-openjdk-17 AS builder

# 指定工作目录
WORKDIR /build

# 第一步：只复制 pom.xml，并下载依赖
# (这样做的好处是，只要你不改 pom.xml，下次打包就会直接使用缓存，极其快)
COPY pom.xml .
RUN mvn dependency:go-offline

# 第二步：复制 src 源代码，并执行打包命令 (-DskipTests 跳过测试以加快打包速度)
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# 第二阶段：最终运行阶段 (极其纯净)
# ==========================================
# 换回你原来的轻量级环境
FROM eclipse-temurin:17-jdk

# 指定工作目录
WORKDIR /app

# 最神奇的一步：从上面的 builder 阶段，把生成的 jar 包“偷”过来！
# 注意：这里的路径 /build/target/... 要和你第一阶段的工作目录对应
COPY --from=builder /build/target/User-1.0-SNAPSHOT.jar app.jar

# 暴露 8080 端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]