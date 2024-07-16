FROM eclipse-temurin:21.0.3_9-jdk as builder

WORKDIR application
ADD ./target/classes/.env ./
ADD ./target/*.jar ./

# Extracts the layers from the jar file
RUN . ./.env && java -Djarmode=layertools -jar $JAR_FILE extract

FROM eclipse-temurin:21.0.3_9-jdk

# incorporating best practices from snyk.io
# see: https://snyk.io/blog/best-practices-to-build-java-containers-with-docker/

WORKDIR application
COPY --from=builder application/dependencies/ ./
# Workaround for Docker bug failing builds. See: https://stackoverflow.com/questions/51115856/docker-failed-to-export-image-failed-to-create-image-failed-to-get-layer
RUN true
COPY --from=builder application/spring-boot-loader/ ./
RUN true
COPY --from=builder application/snapshot-dependencies/ ./
RUN true
COPY --from=builder application/application/ ./

RUN apt-get update &&  apt-get install dumb-init

RUN addgroup --system javauser && \
  adduser --system --shell /bin/false --no-create-home --ingroup javauser javauser  && \
  chown -R javauser:javauser /application

USER javauser

ENTRYPOINT ["dumb-init", "java", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]