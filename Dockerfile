FROM maven:3.9-eclipse-temurin-17-focal AS build_step
RUN mkdir /build
COPY . /build
WORKDIR /build
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-focal

# Install chrome
RUN apt-get update; apt-get clean
RUN apt-get install -y wget
RUN apt-get install -y gnupg
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list
RUN apt-get update && apt-get -y install google-chrome-stable

RUN mkdir /app && \
    mkdir /app/target && \
    mkdir /app/target/dependency && \
    mkdir /app/bin

COPY --from=build_step /build/target/dflmngr.jar \
                       /app/target/
COPY --from=build_step /build/target/dependency/*.jar \
                       /app/target/dependency/
COPY --from=build_step /build/bin/*.sh \
                       /app/bin/

RUN mkdir $HOME/.ssh && chmod 700 $HOME/.ssh

WORKDIR /app
CMD java -classpath /app/target/dflmngr.jar:/app/target/dependency/* net.dflmngr.scheduler.JobScheduler

