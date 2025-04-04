FROM openjdk:21-slim

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

USER spring:spring

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"] 