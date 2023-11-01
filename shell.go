package main

import (
	"bytes"
	"fmt"
	"github.com/pterm/pterm"
	"os/exec"
	"path/filepath"
	"strings"
)

func ConvertToWebP(inputFile string, outputFile string) {
	_, _ = runCommand("cwebp %s -o %s", inputFile, outputFile)
}

func ConvertToWebM(inputFile string, outputFile string) {
	var command string

	if filepath.Ext(inputFile) == ".gif" {
		command = "ffmpeg -i %s -row-mt 1 -movflags faststart -pix_fmt yuv420p -vf scale=trunc(iw/2)*2:trunc(ih/2)*2 %s"
	} else {
		command = "ffmpeg -i %s -row-mt 1 %s"
	}

	_, _ = runCommand(command, inputFile, outputFile)
}

func CheckDeps() {
	dependencies := map[string]string{
		"yt-dlp":  "yt-dlp --version",
		"libwebp": "cwebp -version",
		"FFmpeg":  "ffmpeg -version",
	}

	for name, cmd := range dependencies {
		padding := 10 - len(name)
		pterm.Print("Checking dependency " + pterm.Bold.Sprintf(name) +
			pterm.Sprintf(" %s ", strings.Repeat(".", padding)))

		_, err := runCommand(cmd)
		if err == nil {
			pterm.Println("✅")
		} else {
			pterm.Println("❌")
		}
	}
}

// region - Private functions

func runCommand(command string, a ...interface{}) (string, error) {
	cli := strings.Split(fmt.Sprintf(command, a...), " ")
	cmd := exec.Command(cli[0], cli[1:]...)

	var out bytes.Buffer
	cmd.Stdout = &out

	err := cmd.Run()
	return out.String(), err
}

// endregion