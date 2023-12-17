package main

import (
	"fmt"
	"github.com/go-resty/resty/v2"
	"os"
	"os/user"
	"path/filepath"
	"strings"
)

var client = resty.New()

func IsOutdated(currentVersion string, repo string) bool {
	var tags []Tag
	var url = fmt.Sprintf("https://api.github.com/repos/%s/tags", repo)

	resp, err := client.R().SetResult(&tags).Get(url)
	if resp.StatusCode() != 200 || err != nil {
		return true
	}

	return currentVersion != tags[0].Name
}

func ExpandPath(path string) (string, error) {
	if strings.HasPrefix(path, "~") {
		usr, err := user.Current()
		if err != nil {
			return "", err
		}
		return strings.Replace(path, "~", usr.HomeDir, 1), nil
	}
	return path, nil
}

func CreateReport(directory string, downloads []Download) {
	filePath := filepath.Join(directory, "_report.md")
	file, err := os.Create(filePath)
	if err != nil {
		return
	}

	defer file.Close()

	// Filter the failed downloads
	failedDownloads := make([]Download, 0)
	for _, download := range downloads {
		if !download.IsSuccess {
			failedDownloads = append(failedDownloads, download)
		}
	}

	fileContent := "# Reddit - Download Report\n"
	fileContent += "## Failed Downloads\n"
	fileContent += fmt.Sprintf("- Total: %d\n", len(failedDownloads))

	for _, download := range failedDownloads {
		fileContent += fmt.Sprintf("### 🔗 Link: %s - ❌ **Failure**\n", download.Url)
		fileContent += "### 📝 Error:\n"
		fileContent += "```\n"
		fileContent += fmt.Sprintf("%s\n", download.Error)
		fileContent += "```\n"
		fileContent += "---\n"
	}

	_, _ = file.WriteString(fileContent)
}
