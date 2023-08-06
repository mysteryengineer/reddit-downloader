# RMD (Reddit Media Downloader)

Download all the picture/video posts from a particular user on Reddit. It also removes duplicates and convert files to a better formats, with higher image quality and lower file sizes.

**✨ Works with new Reddit API restrictions of July 1st 2023!**

## 🖼️ Usage

Install Docker in your computer and run the command below:

```
$ docker run --rm -t \
    -e RMD_USER=atomicbrunette18 \
    -e RMD_LIMIT=1000 \
    -e RMD_PARALLEL=5 \
    -e RMD_CONVERT_GIFS=true \
    -v "/path/in/your/computer:/tmp/rmd" \
    vegidio/rmd
```

Where:

- `-e RMD_USER` (mandatory): is the Reddit username that you want to download images/videos from.
- `-e RMD_LIMIT` (optional): the maximum number of posts that you want to query for media files; default `1000`.
- `-e RMD_PARALLEL` (optional): the number of downloads to be done in parallel; default is `5`.
- `-e RMD_CONVERT_IMAGES` (optional): `true/false`, if you want to convert image files to WebP; default `true`.
- `-e RMD_CONVERT_VIDEOS` (optional): `true/false`, if you want to convert video files to WebM; default `false`.

## 🎥 Demo

[![asciicast](https://asciinema.org/a/RSDb5W85jgF51Z4fx0jjd4avE.svg)](https://asciinema.org/a/RSDb5W85jgF51Z4fx0jjd4avE)

## 💡 Identical Files

This application will automatically delete all files that are identical.

## 🛠️ Build

In the project's root folder run in the CLI:

```
$ docker build -t vegidio/rmd .
```

## 📝 License

**RMD** is released under the MIT License. See [LICENSE](LICENSE) for details.

## 👨🏾‍💻 Author

Vinicius Egidio ([vinicius.io](http://vinicius.io))