package main

import (
	"crypto/sha256"
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/pterm/pterm"
	"github.com/spf13/afero"
	"net/http"
	"path/filepath"
	"sort"
	"sync"
	"time"
)

func DownloadMedias(submissions []Submission, directory string, parallel int) []Download {
	var mu sync.Mutex     // mutex to protect shared data
	var wg sync.WaitGroup // wait group to wait for all goroutines to complete
	sem := make(chan struct{}, parallel)

	downloads := make([]Download, 0)

	pb, _ := pterm.DefaultProgressbar.
		WithTotal(len(submissions)).
		Start("Downloading")

	for i, submission := range submissions {
		wg.Add(1)

		// Start a new goroutine
		go func(index int, submission Submission) {
			defer wg.Done()

			sem <- struct{}{} // acquire a semaphore token
			filePath := createFilePath(submission, index+1, directory)
			err := downloadMedia(submission, filePath)

			mu.Lock()
			downloads = append(downloads, getDownloadInfo(submission.Url, filePath, err))
			mu.Unlock()

			pb.Increment()
			<-sem
		}(i, submission)
	}

	wg.Wait()
	close(sem)

	sort.Sort(ByFilePath(downloads))
	return downloads
}

// region - Private functions

func createFilePath(submission Submission, index int, directory string) string {
	t := time.Unix(submission.Created, 0)

	// Go uses a specific layout to represent the time format
	// It is based on the time: Mon Jan 2 15:04:05 MST 2006
	formattedTime := t.Format("20060102-150405")

	fileName := fmt.Sprintf("%s-%s-%d", formattedTime, submission.Author, index)
	extension := filepath.Ext(submission.Url)

	if extension != "" {
		fileName += extension
	} else {
		if submission.MediaType() == Image {
			fileName += ".jpg"
		} else {
			fileName += ".mp4"
		}
	}

	return filepath.Join(directory, fileName)
}

func downloadMedia(submission Submission, filePath string) error {
	var err error

	if submission.MediaType() == Image {
		err = downloadImage(submission.Url, filePath)
	} else {
		err = downloadVideo(submission.Url, filePath)
	}

	return err
}

func downloadImage(url string, filePath string) error {
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

func downloadVideo(url string, outputFile string) error {
	output, err := runCommand("yt-dlp %s -o %s", url, outputFile)

	if err != nil {
		err = fmt.Errorf(output)
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
