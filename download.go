package main

import (
	"crypto/sha256"
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/pterm/pterm"
	"github.com/spf13/afero"
	"net/http"
	"sync"
	"time"
)

func DownloadMedias(medias []Media, parallel int) []Download {
	var mu sync.Mutex     // mutex to protect shared data
	var wg sync.WaitGroup // wait group to wait for all goroutines to complete
	sem := make(chan struct{}, parallel)

	downloads := make([]Download, 0)

	pb, _ := pterm.DefaultProgressbar.
		WithTotal(len(medias)).
		Start("Downloading")

	for _, media := range medias {
		wg.Add(1)

		// Start a new goroutine
		go func(media Media) {
			defer wg.Done()

			sem <- struct{}{} // acquire a semaphore token
			err := downloadFile(media.Url, media.FilePath)

			mu.Lock()
			downloads = append(downloads, getDownloadInfo(media.Url, media.FilePath, err))
			mu.Unlock()

			pb.Increment()
			<-sem
		}(media)
	}

	wg.Wait()
	close(sem)

	return downloads
}

// region - Private functions

func downloadFile(url string, filePath string) error {
	resp, err := client.
		SetRetryCount(3).
		SetRetryWaitTime(5 * time.Second).
		AddRetryCondition(
			func(r *resty.Response, err error) bool {
				return err != nil || r.StatusCode() == http.StatusTooManyRequests
			},
		).
		R().
		SetOutput(filePath).
		Get(url)

	if resp.StatusCode() != 200 {
		return fmt.Errorf("Error - HTTP %d", resp.StatusCode())
	}

	return err
}

func getDownloadInfo(fileUrl string, filePath string, err error) Download {
	fs := afero.NewOsFs()
	file, _ := afero.ReadFile(fs, filePath)
	hash := sha256.Sum256(file)

	download := Download{
		Url:       fileUrl,
		FilePath:  filePath,
		Error:     err,
		IsSuccess: err == nil,
		Hash:      fmt.Sprintf("%x", hash),
	}

	return download
}

// endregion
