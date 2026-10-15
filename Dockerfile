# 1. AŞAMA: Derleme (Build)
# Java 21 ve Maven içeren resmi imajı kullanıyoruz. Bu aşamaya "builder" diyoruz.
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Projemizin ayar dosyasını (pom.xml) ve kodlarını (src) kopyalıyoruz
COPY pom.xml .
COPY src ./src

# Projeyi derleyip .jar dosyasını oluşturuyoruz (Hızlı olması için testleri atlıyoruz)
RUN mvn clean package -DskipTests

# 2. AŞAMA: Çalıştırma (Run)
# Çalıştırmak için sadece hafif bir Java 21 JRE yeterli. Maven'a ihtiyacımız yok.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# İlk aşamada (builder) üretilen .jar dosyasını alıp bu son imaja koyuyoruz
COPY --from=builder /app/target/*.jar app.jar

# Uygulamanın hangi portta çalıştığını belirtiyoruz (Genelde Spring Boot 8080 kullanır)
EXPOSE 8080

# Konteyner ayağa kalktığında çalıştırılacak komut
ENTRYPOINT ["java", "-jar", "app.jar"]
