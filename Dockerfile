FROM eclipse-temurin:17-jdk-jammy AS base
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src src

FROM base AS build
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

# Install chrome
RUN apt-get update && apt-get install -y wget gnupg && apt-get clean
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list
RUN apt-get update && apt-get -y install google-chrome-stable && apt-get clean

WORKDIR /app

COPY --from=build /app/target/dflmngr.jar target/
COPY --from=build /app/target/dependency/*.jar dependency/target/
COPY bin/*.sh bin/

RUN mkdir "$HOME"/.ssh && chmod 700 "$HOME"/.ssh

CMD java -classpath /app/target/dflmngr.jar:/app/target/dependency/* net.dflmngr.scheduler.JobScheduler