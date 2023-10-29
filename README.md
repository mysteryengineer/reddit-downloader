# Reddit Downloader

Download all the picture/video posts from a particular user on Reddit. It also removes duplicates and convert files to better formats, with higher image quality and smaller file sizes.

**✨ Works with new Reddit API restrictions of July 1st 2023!**

## 🎥 Demo

[![asciicast](https://asciinema.org/a/4tayss7Tgxc6UL0qOoXFnAxxW.svg)](https://asciinema.org/a/4tayss7Tgxc6UL0qOoXFnAxxW)

## 🖼️ Usage

Install [Docker](https://docs.docker.com/get-docker/) in your computer, then run the command below:

```
$ docker run --rm -t \
    -e RMD_USER=atomicbrunette18 \
    -e RMD_LIMIT=1000 \
    -e RMD_PARALLEL=5 \
    -e RMD_CONVERT_IMAGES=true \
    -e RMD_CONVERT_VIDEOS=true \
    -v "/path/in/your/computer:/tmp/rmd" \
    vegidio/reddit-downloader
```

Where:

- `-e RMD_USER` (mandatory): is the Reddit username that you want to download images/videos from.
- `-e RMD_LIMIT` (optional): the max number of posts that you want to query for media files; default `1000`.
- `-e RMD_PARALLEL` (optional): the number of downloads to be done in parallel; default is `5`.
- `-e RMD_CONVERT_IMAGES` (optional): `true/false`, if you want to convert images to WebP; default `true`.
- `-e RMD_CONVERT_VIDEOS` (optional): `true/false`, if you want to convert videos to WebM; default `true`.

### Volume

For those that are not familiar with Docker, the `-v` (volume) parameter defines where the media will be saved, and it's divided in two parts, separated by the colon sign `:`. You just need to worry about the first part, on the left side of the colon sign (**don't change anything on the right side**) and update it according to a path in your computer where you want the media to be downloaded.

For example, if you want to download the media in the directory `/Downloads/Reddit` then the volume parameter should look like this `-v "/Downloads/Reddit:/tmp/rmd"`.

## 💡 Identical Files

This application will automatically delete all files that are identical.

## 🛠️ Build

In the project's root folder run in the CLI:

```
$ docker build -t vegidio/reddit-downloader .
```

## 📈 Telemetry

This Docker image collects information about the data that you're downloading to help me track bugs and improve the general stability of the software.

**No identifiable information about you or your computer is tracked.** But if you still want to stop the telemetry, you can do that by adding the environment variable `-e RMD_TELEMETRY=false` when you run the container.

## 📝 License

**RMD** is released under the MIT License. See [LICENSE](LICENSE) for details.

## 👨🏾‍💻 Author

Vinicius Egidio ([vinicius.io](http://vinicius.io))