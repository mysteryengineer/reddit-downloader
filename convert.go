package main

import (
	"github.com/pterm/pterm"
	"github.com/spf13/afero"
	"github.com/thoas/go-funk"
	"path/filepath"
	"strings"
	"sync"
)

var fs = afero.NewOsFs()

func ConvertMedia(downloads []Download, convertImages bool, convertVideos bool) {
	var wg sync.WaitGroup // wait group to wait for all goroutines to complete
	sem := make(chan struct{}, 5)

	if !convertImages {
		downloads = funk.Filter(downloads, func(download Download) bool {
			return download.MediaType() != Image
		}).([]Download)
	}

	if !convertVideos {
		downloads = funk.Filter(downloads, func(download Download) bool {
			return download.MediaType() != Video
		}).([]Download)
	}

	if len(downloads) > 0 {
		pterm.Println("\n⚙️ Converting media...")
	}

	for _, media := range downloads {
		wg.Add(1)

		go func(download Download) {
			defer wg.Done()
			sem <- struct{}{} // acquire a semaphore token

			var outputFile string
			if download.MediaType() == Image {
				outputFile = replaceExtension(download.FilePath, ".avif")
				fileName := filepath.Base(download.FilePath)

				pterm.Printf("["+pterm.Green("C")+"] Converting %s to %s...\n", pterm.Bold.Sprintf(fileName),
					pterm.Magenta("AVIF"))
				ConvertToAvif(download.FilePath, outputFile)

			} else if download.MediaType() == Video {
				outputFile = replaceExtension(download.FilePath, ".mkv")
				fileName := filepath.Base(download.FilePath)

				pterm.Printf("["+pterm.Green("C")+"] Converting %s to %s...\n", pterm.Bold.Sprintf(fileName),
					pterm.Yellow("AV1"))
				ConvertToAv1(download.FilePath, outputFile)
			}

			// If the file was converted successfully, then we delete the original file
			if fileExists(outputFile) && fileSize(outputFile) > 0 {
				_ = fs.Remove(download.FilePath)
			}

			<-sem
		}(media)
	}

	wg.Wait()
	close(sem)
}

func RemoveDuplicates(downloads []Download) (int, []Download) {
	numDeleted := 0
	remaining := make([]Download, 0)
	duplicates := make(map[string][]Download)

	for _, download := range downloads {
		duplicates[download.Hash] = append(duplicates[download.Hash], download)
	}

	pterm.Println("\n🚮 Removing duplicated downloads...")

	for _, value := range duplicates {
		remaining = append(remaining, value[0])
		deleteList := value[1:]

		for _, deleteFile := range deleteList {
			fileName := filepath.Base(deleteFile.FilePath)
			pterm.Printf("["+pterm.LightRed("D")+"] Deleting %s...\n", pterm.Bold.Sprintf(fileName))
			numDeleted++
			_ = fs.Remove(deleteFile.FilePath)
		}
	}

	return numDeleted, remaining
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

func fileSize(filePath string) int64 {
	file, err := fs.Open(filePath)
	if err != nil {
		return -1
	}

	defer file.Close()

	fileInfo, err := file.Stat()
	if err != nil {
		return -1
	}

	return fileInfo.Size()
}

// endregion
