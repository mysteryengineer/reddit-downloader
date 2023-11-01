package main

import (
	"encoding/json"
	"strings"
)

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

type Submission struct {
	Domain   string `json:"domain"`
	Url      string `json:"url"`
	PostHint string `json:"post_hint"`
	Created  int64  `json:"created_utc"`
}

type Download struct {
	Url       string
	FilePath  string
	Error     error
	IsSuccess bool
	Hash      string
}

// region - Methods

func (s *Submission) MediaType() MediaType {
	if hasSuffix(s.Url, ".jpg") || hasSuffix(s.Url, ".jpeg") {
		return Image
	} else if hasSuffix(s.Url, ".png") || hasSuffix(s.Url, ".gif") {
		return Image
	} else if hasSuffix(s.Url, ".gifv") || hasSuffix(s.Url, ".mp4") || hasSuffix(s.Url, ".m4v") {
		return Video
	} else if s.PostHint == "image" {
		return Image
	}

	return Video
}

func hasSuffix(s string, suffix string) bool {
	return strings.HasSuffix(strings.ToLower(s), strings.ToLower(suffix))
}

// endregion

// region - Custom Unmarshal

func (s *Submission) UnmarshalJSON(data []byte) error {
	type submissionAlias struct {
		Domain   string `json:"domain"`
		Url      string `json:"url"`
		PostHint string `json:"post_hint"`
	}

	var alias submissionAlias
	if err := json.Unmarshal(data, &alias); err != nil {
		return err
	}

	// Copy the values from alias to the actual struct
	s.Domain = alias.Domain
	s.Url = alias.Url
	s.PostHint = alias.PostHint

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

// region - Sort

type ByFilePath []Download

func (a ByFilePath) Len() int           { return len(a) }
func (a ByFilePath) Swap(i, j int)      { a[i], a[j] = a[j], a[i] }
func (a ByFilePath) Less(i, j int) bool { return a[i].FilePath < a[j].FilePath }

// endregion