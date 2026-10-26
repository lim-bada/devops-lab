# 1단계: build. JDK 가 있는 이미지에서 jar 를 만든다
# 이미지 이름을 docker.io/library/ 부터 다 적는다. buildah(5장)는 짧은 이름을 거부한다
FROM docker.io/library/eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null   # 의존성만 먼저 받아 layer cache 를 살린다
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2단계: 실행. JRE 만 있는 작은 이미지에 jar 만 옮긴다
FROM docker.io/library/eclipse-temurin:21-jre
# CI 가 --build-arg APP_VERSION=<git sha> 로 넘긴다. /api/hello 의 version 에 그대로 찍힌다
ARG APP_VERSION=local
ENV APP_VERSION=$APP_VERSION
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/build/libs/app.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
