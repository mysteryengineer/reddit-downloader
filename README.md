# RMD (Reddit Media Downloader)

Download all the picture/video posts from a particular user on Reddit, for uh... reasons. Automatically removes duplicates.

## 🖼️ Usage

Install Docker in your computer and run the command below:

```
$ docker run --rm -t \
    -e RMD_USER=atomicbrunette18 \
    -e RMD_LIMIT=100 \
    -e RMD_SIMILAR=A \
    -v "/path/in/your/computer:/tmp/rmd" \
    vegidio/rmd
```

Where:

- `-e RMD_USER` (mandatory): is the Reddit username that you want to download images/videos from.
- `-e RMD_LIMIT` (mandatory): the maximum number of posts that you want to query for media files.
- `-e RMD_PARALLEL` (optional): the number of downloads to be done in parallel; default is 5.
- `-e RMD_SIMILAR` (optional): the criteria to exclude similar files: `I` (images only), `V` (videos only), `A` (all).

## 💡 Similar Files

This application will automatically delete all duplicated files that are identical, but sometimes there are medias that are very similar, but not necessarily identical. It could be, for example, two photos that were taken very close to each other, with slightly different angles.

If you set the parameter `RMD_SIMILAR` above then it will attempt to identify similar media files to exclude, but keep in mind that:
- (1) the criteria of what is considered similar or not is subjective so this could end up deleting or (not deleting) files that you would expect otherwise.
- (2) if you use the parameter `V` or `A`, which include the detection of similar videos, be aware that it could take a long time to identify similar videos depending on the number of files that were downloaded.

## 🛠️ Build

In the project's root folder run in the CLI:

```
$ docker build -t vegidio/rmd .
```

## 📝 License

**RMD** is released under the MIT License. See [LICENSE](LICENSE) for details.

## 👨🏾‍💻 Author

Vinicius Egidio ([vinicius.io](http://vinicius.io))