### Build Image ###
FROM --platform=$BUILDPLATFORM alpine:edge AS BUILD_IMAGE
ARG TARGETARCH
ARG VERSION

# Installing UnZip
RUN apk add --no-cache go --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Build project
COPY . /reddit
WORKDIR /reddit
RUN sed -i "s/<version>/$VERSION/g" main.go
RUN GOARCH=$TARGETARCH go build -o reddit-dl

### Main Image ###
FROM alpine:edge
LABEL org.opencontainers.image.source="https://github.com/mysteryengineer/reddit-downloader"

# Dependencies
RUN apk add --no-cache yt-dlp libavif-apps ffmpeg --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community

# Define the image version
ENV IMAGE_VERSION=$VERSION

COPY --from=BUILD_IMAGE /reddit/reddit-dl /usr/local/bin/

CMD ["reddit-dl", "-d", "/tmp/reddit"]