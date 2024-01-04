# Reddit Downloader

Download all the pictures/videos posts from a particular user on Reddit. It also removes duplicates and convert files to better formats, with higher image quality and smaller file sizes.

**✨ Works with new Reddit API restrictions of July 1st 2023!**

## 🎥 Demo

[![asciicast](https://asciinema.org/a/I1kPDU24ecJF0mNKspBCqzMGd.svg)](https://asciinema.org/a/I1kPDU24ecJF0mNKspBCqzMGd)

## 🖼️ Usage

There are 2 ways to use this app: through the CLI tool or using Docker. Here are some points to consider to help you choose which solution is best for you:

1. **CLI tool**: this is quick and easier way to use __reddit-dl__ however you must make sure that you have some dependencies installed in your computer before using it.

    - *Dependencies:* Install [yt-dlp](https://github.com/yt-dlp/yt-dlp#installation) (required) and [libavif](https://github.com/AOMediaCodec/libavif) / [FFmpeg](https://www.ffmpeg.org/download.html) (both optional) before using the CLI tool. You can verify if all dependencies are properly installed by running the command `reddit-dl check-deps`.

2. **Docker**: if you don't want to worry about installing dependencies in your computer before using __reddit-dl__ then the Docker version is probably better since it comes with all the dependencies ready to use.

### CLI Tool

Download the [latest version](https://github.com/mysteryengineer/reddit-downloader/releases) of __reddit-dl__ that matches your computer architecture and operating system. Extract the .zip file somewhere and then run the command below in the terminal:

```
$ reddit-dl -s user -n atomicbrunette18 -d /Downloads/Reddit
```

Where:

- `-s` (mandatory): the source type on Reddit where the files are located; `user` or `subreddit`.
- `-n` (mandatory): the name of the user or subreddit you want to download media from.
- `-d` (optional): the directory where you want the files to be saved; default is the current directory.

For the full list of parameters, type `reddit-dl --help` in the terminal.

### Docker

Install [Docker](https://docs.docker.com/get-docker/) in your computer, then run the command below:

```
$ docker run --rm -t \
    -e REDDIT_SOURCE=user \
    -e REDDIT_NAME=atomicbrunette18 \
    -v "/path/in/your/computer:/tmp/reddit" \
    ghcr.io/mysteryengineer/reddit-downloader
```

Where:

- `-e REDDIT_SOURCE`: (mandatory): the source type on Reddit where the files are located; `user` or `subreddit`.
- `-e REDDIT_NAME`: (mandatory): the name of the user or subreddit you want to download media from.

#### Volume

For those that are not familiar with Docker, the `-v` (volume) parameter defines where the media will be saved, and it's divided in two parts, separated by the colon sign `:`. You just need to worry about the first part, on the left side of the colon sign (**don't change anything on the right side**) and update it according to a path in your computer where you want the media to be downloaded.

For example, if you want to download the media in the directory `/Downloads/Reddit` then the volume parameter should look like this `-v "/Downloads/Reddit:/tmp/reddit"`.

## 💡 Features

### Remove duplicates

This application will automatically delete all files that are identical.

### File filtering

You can filter the files that you want to download based on their extension, separated by comma. To do that you must:

- **CLI tool:** add the parameter `--extensions`; for example: `--extensions jpg,jpeg`. 

- **Docker:** add the environment variable `-e REDDIT_EXTENSIONS`; for example: `-e REDDIT_EXTENSIONS=jpg,jpeg`.

### Convert images/videos

You can convert the media downloaded to better formats (AVIF for images and AV1 for videos); this will make the files smaller but preserving the same quality. To do that you must:

- **CLI tool:** add the parameters `--convert-images` and/or `--convert-videos`, depending on what you need.

- **Docker:** add the environment variables `-e REDDIT_CONVERT_IMAGES=true` and/or `-e REDDIT_CONVERT_VIDEOS=true` when you run the container.

## 🛠️ Build

In the project's root folder run in the CLI:

Go:
```
$ go build -o reddit-dl
```

Docker:
```
$ docker build -t ghcr.io/mysteryengineer/reddit-downloader . --build-arg="VERSION=24.1.4"
```

## 📈 Telemetry

This app collects information about the data that you're downloading to help me track bugs and improve the general stability of the software.

**No identifiable information about you or your computer is tracked.** But if you still want to stop the telemetry, you can do that by adding the flag `--no-telemetry` in the CLI tool or the environment variable `-e REDDIT_TELEMETRY=false` when you run the Docker container.

## 📝 License

**reddit-dl** is released under the MIT License. See [LICENSE](LICENSE) for details.