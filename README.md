# RMD (Reddit Media Downloader)

Download all the picture/video posts from a particular user on Reddit, for uh... reasons. Automatically removes duplicates.

## 🖼️ Usage

Install Docker in your computer and run the command below:

```
$ docker run --rm \
    -e RMD_USER=atomicbrunette18 \
    -e RMD_LIMIT=100 \
    -v "/path/in/your/computer:/tmp/rmd" \
    vegidio/rmd
```

Where:

* `RMD_USER` is the Reddit username that you want to download images/videos from.
* `RMD_LIMIT` the maximum amount of files that you want to download.

## 🛠️ Build

In the project's root folder run in the CLI:

```
$ docker build -t vegidio/rmd .
```

## 📝 License

**RMD** is released under the MIT License. See [LICENSE](LICENSE) for details.

## 👨🏾‍💻 Author

Vinicius Egidio ([vinicius.io](http://vinicius.io))