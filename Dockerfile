
# Giai đoạn 1: Build ứng dụng Spring Boot với Java 25
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . .

# Build bằng Maven Wrapper
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# Giai đoạn 2: Chạy ứng dụng
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Giới hạn bộ nhớ JVM phù hợp Render Free
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:ActiveProcessorCount=1"

EXPOSE 10000

# Sử dụng cổng do Render cung cấp
CMD ["sh", "-c", "java -Dserver.port=${PORT:-10000} -jar app.jar"]
