FROM gradle:8.8-jdk17 AS build
COPY --chown=gradle:gradle ./ /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle bootJar -x test --no-daemon


FROM openjdk:17-jdk-slim
COPY --from=build /home/gradle/project/build/libs/msa-auth-1.0.0.jar /app/app.jar

EXPOSE 8080

# Activate the Kubernetes profile or any other profile as needed
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=k8s"]
