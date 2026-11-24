FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Dependencies first: this layer is reused while build.gradle.kts doesn't change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY newrelic newrelic
COPY src src
RUN ./gradlew bootJar copyNewRelic --no-daemon -q

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
COPY --from=build /app/build/newrelic newrelic
USER app
EXPOSE 8080
# The agent stays off until the environment turns it on with a license key (see .env.example).
ENV SPRING_PROFILES_ACTIVE=production \
    NEW_RELIC_APP_NAME=printscript \
    NEW_RELIC_AGENT_ENABLED=false
ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "/app/app.jar"]
