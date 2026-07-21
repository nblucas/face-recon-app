FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -B package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends libgtk2.0-0 \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --create-home --shell /bin/false appuser \
    && mkdir -p /home/appuser/storage/pictures /home/appuser/.djl.ai \
    && chown -R appuser:appuser /home/appuser
USER appuser
WORKDIR /home/appuser/app
COPY --from=build --chown=appuser:appuser /build/target/*.jar app.jar

ENV APP_STORAGE_PICTURES_DIR=/home/appuser/storage/pictures

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
