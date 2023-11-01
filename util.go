package main

import (
	"fmt"
	"os"
	"os/user"
	"path/filepath"
	"strings"
)

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

func ReplaceExtension(filePath string, newExtension string) string {
	extension := filepath.Ext(filePath)
	return strings.Replace(filePath, extension, newExtension, 1)
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
