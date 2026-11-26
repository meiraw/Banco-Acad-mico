FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 academico \
    && useradd --uid 10001 --gid 10001 --no-create-home academico \
    && install -d -o 10001 -g 10001 -m 0750 /data/uploads
WORKDIR /app
COPY --from=build /build/target/banco.academico-*.jar /app/banco-academico.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/banco-academico.jar"]
