package main

import (
	"fmt"
	"github.com/pterm/pterm"
	"github.com/thoas/go-funk"
	"github.com/urfave/cli/v2"
	"os"
	"path/filepath"
)

func main() {
	var source string
	var name string
	var directory string
	var parallel int
	var limit int
	var noTelemetry bool
	var convertImages bool
	var convertVideos bool

	currentPath, _ := os.Getwd()

	app := &cli.App{
		Name:            "reddit-dl",
		Usage:           "a CLI tool to download files from https://reddit.com",
		UsageText:       "reddit-dl -s [user/subreddit] -n [name] [global options]",
		Version:         "<version>",
		HideHelpCommand: true,
		Flags: []cli.Flag{
			&cli.StringFlag{
				Name:        "source",
				Aliases:     []string{"s"},
				Usage:       "the source type on Reddit where the media is located: 'user' or 'subreddit'",
				Destination: &source,
				Category:    "Required:",
				EnvVars:     []string{"REDDIT_SOURCE"},
				Action: func(context *cli.Context, s string) error {
					if s != "user" && s != "subreddit" {
						return fmt.Errorf("Invalid source '%s'", source)
					}
					return nil
				},
			},
			&cli.StringFlag{
				Name:        "name",
				Aliases:     []string{"n"},
				Usage:       "the name of the subreddit or user profile you want to download media from",
				Destination: &name,
				Category:    "Required:",
				EnvVars:     []string{"REDDIT_NAME"},
			},
			&cli.StringFlag{
				Name:        "dir",
				Aliases:     []string{"d"},
				Value:       currentPath,
				Usage:       "directory where the files will be saved",
				Destination: &directory,
				Category:    "Optional:",
				DefaultText: "current directory",
			},
			&cli.IntFlag{
				Name:        "parallel",
				Value:       5,
				Usage:       "the number of downloads to be done in parallel",
				Destination: &parallel,
				Category:    "Optional:",
				DefaultText: "5",
				EnvVars:     []string{"REDDIT_PARALLEL"},
				Action: func(context *cli.Context, i int) error {
					if i < 1 || i > 10 {
						return fmt.Errorf("The number of parallel downloads should be between 1-10")
					}
					return nil
				},
			},
			&cli.IntFlag{
				Name:        "limit",
				Value:       1_000_000,
				Usage:       "the maximum number of files to be downloaded",
				Destination: &limit,
				Category:    "Optional:",
				EnvVars:     []string{"REDDIT_LIMIT"},
				DefaultText: "all files",
				Action: func(context *cli.Context, i int) error {
					if i < 1 {
						return fmt.Errorf("The number of max downloads should be at least 1")
					}
					return nil
				},
			},
			&cli.BoolFlag{
				Name:               "no-telemetry",
				Value:              false,
				Usage:              "if you want to disable the telemetry",
				Destination:        &noTelemetry,
				Category:           "Optional:",
				DisableDefaultText: true,
				EnvVars:            []string{"REDDIT_TELEMETRY"},
			},
			&cli.BoolFlag{
				Name:               "convert-images",
				Value:              false,
				Usage:              "enable the conversion of images to WebP",
				Destination:        &convertImages,
				Category:           "Optional:",
				DisableDefaultText: true,
				EnvVars:            []string{"REDDIT_CONVERT_IMAGES"},
			},
			&cli.BoolFlag{
				Name:               "convert-videos",
				Value:              false,
				Usage:              "enable the conversion of videos to WebM",
				Destination:        &convertVideos,
				Category:           "Optional:",
				DisableDefaultText: true,
				EnvVars:            []string{"REDDIT_CONVERT_VIDEOS"},
			},
		},
		Action: func(cCtx *cli.Context) error {
			if source == "" {
				return fmt.Errorf("Required flag '--source', '-s' is missing")
			}

			if name == "" {
				return fmt.Errorf("Required flag '--name', '-u' is missing")
			}

			fullDir, err := ExpandPath(filepath.Join(directory, name))
			if err != nil {
				return fmt.Errorf("Directory path %s is invalid", directory)
			}

			err = startJob(
				cCtx.App.Version,
				source,
				name,
				fullDir,
				parallel,
				limit,
				noTelemetry,
				convertImages,
				convertVideos,
			)

			return err
		},
		Commands: []*cli.Command{
			{
				Name:  "check-deps",
				Usage: "check if you have all the dependencies installed in your computer",
				Action: func(cCtx *cli.Context) error {
					CheckDeps()
					return nil
				},
			},
		},
	}

	if err := app.Run(os.Args); err != nil {
		PrintError(err.Error())
	}
}

// region - Private functions

func startJob(
	version string,
	source string,
	name string,
	directory string,
	parallel int,
	limit int,
	noTelemetry bool,
	convertImages bool,
	convertVideos bool,
) error {
	err := CheckSource(source, name)
	if err != nil {
		return err
	}

	if !noTelemetry {
		TrackDownloadStart(version, source, name, parallel, limit, false, false)
	}

	submissions := GetMedias(source, name, limit)

	downloads := DownloadMedias(submissions, directory, parallel)
	successes := funk.Filter(downloads, func(download Download) bool { return download.IsSuccess }).([]Download)
	failures := funk.Filter(downloads, func(download Download) bool { return !download.IsSuccess }).([]Download)

	duplicated := RemoveDuplicates(successes)

	if !noTelemetry {
		TrackDownloadEnd(version, source, name, len(submissions), len(failures), duplicated)
	}

	CreateReport(directory, downloads)

	if convertImages {
		ConvertImages(downloads)
	}

	if convertVideos {
		ConvertVideos(downloads)
	}

	pterm.Println("\n🌟 Done!")
	return nil
}

// region
