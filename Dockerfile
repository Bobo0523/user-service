# 使用包含 Java 的基础环境
FROM eclipse-temurin:17-jdk
# 把你打包好的 jar 包丢进集装箱
COPY target/User-1.0-SNAPSHOT.jar app.jar
# 暴露 8080 端口
EXPOSE 8080
# 启动命令
ENTRYPOINT ["java","-jar","/app.jar"]
