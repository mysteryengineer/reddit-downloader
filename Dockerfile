### Build Image ###
FROM --platform=$BUILDPLATFORM alpine AS BUILD_IMAGE
ARG TARGETPLATFORM

# Installing OpenJDK 17
RUN apk add --no-cache openjdk17 binutils --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Build project
COPY . /rmd
WORKDIR /rmd
RUN ./gradlew shadowJar

### JRE Image ###
FROM alpine AS JRE_IMAGE

# Installing OpenJDK 17
RUN apk add --no-cache openjdk17 binutils --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

WORKDIR /tmp
COPY --from=BUILD_IMAGE /rmd/build/libs/rmd-1.0-all.jar /tmp/rmd.jar
RUN unzip rmd.jar
RUN jdeps --print-module-deps --ignore-missing-deps --recursive --multi-release 17 \
    --class-path="BOOT-INF/lib/*" --module-path="BOOT-INF/lib/*" rmd.jar > /deps.txt

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
RUN apk add --no-cache yt-dlp ffmpeg

# Create custom JRE
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=JRE_IMAGE /customjre $JAVA_HOME

# Define the image version
ARG VERSION
ENV IMAGE_VERSION=$VERSION

COPY --from=BUILD_IMAGE /rmd/build/libs/rmd-1.0-all.jar /var/rmd.jar

ENTRYPOINT ["java", "-jar", "/var/rmd.jar"]