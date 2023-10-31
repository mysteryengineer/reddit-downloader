package main

import (
	"context"
	"github.com/google/uuid"
	"github.com/mixpanel/mixpanel-go"
)

var mp = mixpanel.NewApiClient("5be99deb1f8e8bdd9cca5e8e0a1a15a8")
var distinctId = uuid.New().String()

func TrackDownloadStart(
	version string,
	service string,
	user string,
	parallel int,
	limit int,
	convertImages bool,
	convertVideos bool,
) {
	ctx := context.Background()

	_ = mp.Track(ctx, []*mixpanel.Event{
		mp.NewEvent("Reddit Download Start", distinctId, map[string]any{
			"version":       version,
			"service":       service,
			"user":          user,
			"parallel":      parallel,
			"limit":         limit,
			"convertImages": convertImages,
			"convertVideos": convertVideos,
		}),
	})
}

func TrackDownloadEnd(
	version string,
	service string,
	user string,
	mediaFound int,
	failedDownloads int,
	duplicated int,
) {
	ctx := context.Background()

	_ = mp.Track(ctx, []*mixpanel.Event{
		mp.NewEvent("Reddit Download End", distinctId, map[string]any{
			"version":         version,
			"service":         service,
			"user":            user,
			"mediaFound":      mediaFound,
			"failedDownloads": failedDownloads,
			"duplicated":      duplicated,
		}),
	})
}
