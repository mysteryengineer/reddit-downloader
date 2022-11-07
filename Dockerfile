### Build Image ###
FROM alpine AS BUILD_IMAGE

# Installing OpenJDK 17
RUN apk add --no-cache openjdk17 binutils --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Build project
COPY . /rmd
WORKDIR /rmd
RUN ./gradlew shadowJar

# Creating list of dependencies
WORKDIR /rmd/build/libs
RUN unzip rmd-1.0-all.jar
RUN jdeps --print-module-deps --ignore-missing-deps --recursive --multi-release 17 \
    --class-path="BOOT-INF/lib/*" --module-path="BOOT-INF/lib/*" rmd-1.0-all.jar > /deps.txt

# Build small JRE image
RUN jlink --verbose \
         --add-modules $(cat /deps.txt) \
         --add-modules jdk.crypto.ec \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

### Main Image ###
FROM alpine
LABEL maintainer="Vinicius Egidio <me@vinicius.io>"

# Dependencies
RUN apk add --no-cache youtube-dl ffmpeg

# Create custom JRE
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=BUILD_IMAGE /customjre $JAVA_HOME

# Define the image version
ARG VERSION
ENV IMAGE_VERSION=$VERSION

COPY --from=BUILD_IMAGE /rmd/build/libs/rmd-1.0-all.jar /var/rmd.jar

ENTRYPOINT ["java", "-jar", "/var/rmd.jar"]