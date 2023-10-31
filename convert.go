package main

import (
	"github.com/pterm/pterm"
	"github.com/spf13/afero"
	"github.com/thoas/go-funk"
	"path/filepath"
	"strings"
)

var fs = afero.NewOsFs()

func ConvertImages(downloads []Download) {
	filteredDownloads := funk.Filter(downloads, func(download Download) bool {
		extension := filepath.Ext(download.FilePath)
		return extension == ".jpg" || extension == ".jpeg" || extension == ".png"
	}).([]Download)

	if len(filteredDownloads) > 0 {
		pterm.Println("\n⚙️ Converting images to WebP...")
	}

	for _, images := range filteredDownloads {
		outputFile := replaceExtension(images.FilePath, ".webp")

		pterm.Printf("["+pterm.Green("C")+"] Converting %s to WebP...\n", images.FilePath)
		ConvertToWebP(images.FilePath, outputFile)

		// If the file was converted successfully, then we delete the original file
		if fileExists(images.FilePath) {
			_ = fs.Remove(images.FilePath)
		}
	}
}

func ConvertVideos(downloads []Download) {
	filteredDownloads := funk.Filter(downloads, func(download Download) bool {
		extension := filepath.Ext(download.FilePath)
		return extension == ".gif" || extension == ".mp4" || extension == ".m4v"
	}).([]Download)

	if len(filteredDownloads) > 0 {
		pterm.Println("\n⚙️ Converting videos to WebM...")
	}

	for _, video := range filteredDownloads {
		outputFile := replaceExtension(video.FilePath, ".webm")

		pterm.Printf("["+pterm.Green("C")+"] Converting %s to WebM...\n", video.FilePath)
		ConvertToWebM(video.FilePath, outputFile)

		// If the file was converted successfully, then we delete the original file
		if fileExists(video.FilePath) {
			_ = fs.Remove(video.FilePath)
		}
	}
}

func RemoveDuplicates(downloads []Download) int {
	numDeleted := 0

	duplicates := make(map[string][]Download)
	for _, download := range downloads {
		duplicates[download.Hash] = append(duplicates[download.Hash], download)
	}

	pterm.Println("\n🚮 Removing duplicated downloads...")

	for _, value := range duplicates {
		deleteList := value[1:]

		for _, deleteFile := range deleteList {
			pterm.Printf("["+pterm.LightRed("D")+"] %s ...\n", deleteFile.FilePath)
			numDeleted++
			_ = fs.Remove(deleteFile.FilePath)
		}
	}

	return numDeleted
}

// region - Private functions

func replaceExtension(filePath string, newExtension string) string {
	extension := filepath.Ext(filePath)
	return strings.Replace(filePath, extension, newExtension, 1)
}

func fileExists(filePath string) bool {
	exists, err := afero.Exists(fs, filePath)
	if err != nil {
		return false
	}

	return exists
}

// endregion
