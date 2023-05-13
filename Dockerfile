FROM maven:3.9-eclipse-temurin-17-focal AS build_step
RUN mkdir /build
COPY . /build
WORKDIR /build
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-focal

# Install chrome
RUN apt-get install -y wget
RUN wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN apt-get install ./google-chrome-stable_current_amd64.deb

RUN mkdir /app && \
    mkdir /app/target

COPY --from=build_step /build/target/dflmngr.jar \
                       /build/tager/dependency \
                       /app/target/
COPY --from=build_step /build/bin \
                       /app/

WORKDIR /app
CMD java -classpath /app/target/dflmngr.jar:/app/target/dependency/* net.dflmngr.scheduler.JobScheduler

