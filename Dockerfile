FROM mcr.microsoft.com/devcontainers/java:21 AS build
ENV JAVA_HOME=/usr/lib/jvm/msopenjdk-21
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /workspace
COPY . .
RUN java -version
RUN ./gradlew --no-daemon clean standaloneJar

FROM mcr.microsoft.com/devcontainers/java:21
ENV JAVA_HOME=/usr/lib/jvm/msopenjdk-21
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app
COPY --from=build /workspace/build/libs/fitnesse-*-standalone.jar /app/fitnesse.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/fitnesse.jar", "-p", "8080", "-e", "0"]
