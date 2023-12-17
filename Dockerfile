### Build Image ###
FROM --platform=$BUILDPLATFORM alpine:edge AS BUILD_IMAGE
ARG TARGETARCH
ARG VERSION

# Dependencies
RUN apk add --no-cache wget --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Download the binary
RUN mkdir /reddit
WORKDIR /reddit
RUN wget https://github.com/mysteryengineer/reddit-downloader/releases/download/$VERSION/reddit-dl_linux_$TARGETARCH.zip
RUN unzip reddit-dl_linux_$TARGETARCH.zip

### Main Image ###
FROM alpine:edge
LABEL org.opencontainers.image.source="https://github.com/mysteryengineer/reddit-downloader"

# Dependencies
RUN apk add --no-cache yt-dlp libavif-apps ffmpeg --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Define the image version
ENV IMAGE_VERSION=$VERSION

COPY --from=BUILD_IMAGE /reddit/reddit-dl /usr/local/bin/

CMD ["reddit-dl", "-d", "/tmp/reddit"]