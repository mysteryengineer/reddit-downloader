### Build Image ###
FROM --platform=$BUILDPLATFORM alpine:edge AS BUILD_IMAGE
ARG TARGETPLATFORM

# Installing OpenJDK 17
RUN apk add --no-cache openjdk17 --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Build project
COPY . /rmd
WORKDIR /rmd
RUN ./gradlew nativeBinaries

### Main Image ###
FROM alpine:edge
LABEL maintainer="Vinicius Egidio <me@vinicius.io>"

# Dependencies
RUN apk add --no-cache ffmpeg && \
    apk add --no-cache yt-dlp --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Define the image version
ARG VERSION
ENV IMAGE_VERSION=$VERSION

COPY --from=BUILD_IMAGE /rmd/build/bin/native/releaseExecutable/rmd.kexe /var/rmd.kexe

ENTRYPOINT ["/var/rmd.kexe"]