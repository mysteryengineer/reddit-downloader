package main

import (
	"encoding/json"
	"net/url"
	"path/filepath"
	"strings"
)

type Tag struct {
	Name string `json:"name"`
}

type Response struct {
	Data ResponseData `json:"data"`
}

type ResponseData struct {
	After    string     `json:"after"`
	Children []Children `json:"children"`
}

type Children struct {
	Data Submission `json:"data"`
}

// region - Submission

type Submission struct {
	Author        string                 `json:"author"`
	Domain        string                 `json:"domain"`
	Url           string                 `json:"url"`
	PostHint      string                 `json:"post_hint"`
	Created       int64                  `json:"created_utc"`
	IsGallery     bool                   `json:"is_gallery"`
	MediaMetadata map[string]interface{} `json:"media_metadata"`
}

func (s *Submission) MediaType() MediaType {
	if hasSuffix(s.Url, ".jpg") || hasSuffix(s.Url, ".jpeg") {
		return Image
	} else if hasSuffix(s.Url, ".png") || hasSuffix(s.Url, ".gif") {
		return Image
	} else if hasSuffix(s.Url, ".gifv") || hasSuffix(s.Url, ".mp4") || hasSuffix(s.Url, ".m4v") {
		return Video
	} else if s.PostHint == "image" || s.IsGallery {
		return Image
	}

	return Video
}

func (s *Submission) Ext() string {
	parsedUrl, _ := url.Parse(s.Url)
	extension := strings.ToLower(filepath.Ext(parsedUrl.Path))

	if extension != "" {
		return extension
	} else {
		if s.MediaType() == Image {
			return ".jpg"
		} else {
			return ".mp4"
		}
	}
}

func (s *Submission) UnmarshalJSON(data []byte) error {
	type submissionAlias struct {
		Author        string                 `json:"author"`
		Domain        string                 `json:"domain"`
		Url           string                 `json:"url"`
		PostHint      string                 `json:"post_hint"`
		IsGallery     bool                   `json:"is_gallery"`
		MediaMetadata map[string]interface{} `json:"media_metadata"`
	}

	var alias submissionAlias
	if err := json.Unmarshal(data, &alias); err != nil {
		return err
	}

	// Copy the values from alias to the actual struct
	s.Author = alias.Author
	s.Domain = alias.Domain
	s.Url = alias.Url
	s.PostHint = alias.PostHint
	s.IsGallery = alias.IsGallery
	s.MediaMetadata = alias.MediaMetadata

	// Handle "Created" separately
	var raw map[string]interface{}
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}

	if val, ok := raw["created_utc"].(float64); ok {
		s.Created = int64(val)
	}

	return nil
}

// endregion

// region - Download

type Download struct {
	Url       string
	FilePath  string
	Error     error
	IsSuccess bool
	Hash      string
}

func (d *Download) MediaType() MediaType {
	extension := filepath.Ext(d.FilePath)

	if extension == ".jpg" || extension == ".jpeg" || extension == ".png" {
		return Image
	} else if extension == ".gif" || extension == ".gifv" || extension == ".mp4" || extension == ".m4v" {
		return Video
	}

	return Unknown
}

type ByFilePath []Download

func (a ByFilePath) Len() int           { return len(a) }
func (a ByFilePath) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a ByFilePath) Less(i, j int) bool { return a[i].FilePath < a[j].FilePath }

// endregion

// region - Methods

func hasSuffix(s string, suffix string) bool {
	return strings.HasSuffix(strings.ToLower(s), strings.ToLower(suffix))
}

// endregion
