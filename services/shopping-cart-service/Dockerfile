FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/shopping-cart-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
