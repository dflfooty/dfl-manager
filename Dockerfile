FROM maven:3.9-eclipse-temurin-17-focal AS build_step
RUN mkdir /build
COPY . /build
WORKDIR /build
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-focal
RUN mkdir /app && \
    mkdir /app/lib

COPY --from=build_step /build/target/dflmngr.jar \
                       /build/bin \
                       /app/
COPY --from=build_step /build/target/dependency/*.jar \
                       /app/lib/

WORKDIR /app
CMD java -classpath /app/dflmngr.jar:/app/lib/dependency/* net.dflmngr.scheduler.JobScheduler

