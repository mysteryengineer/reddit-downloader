package main

import (
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/pterm/pterm"
	"github.com/thoas/go-funk"
)

var client = resty.New()

func CheckSource(source string, name string) error {
	if source == "user" {
		url := createUserUrl(name, "")
		resp, err := client.R().Get(url)

		if resp.StatusCode() != 200 || err != nil {
			return fmt.Errorf("Error fetching user '%s'", name)
		}
	} else {
		url := createSubredditUrl(name, "")
		resp, err := client.R().Get(url)

		if resp.StatusCode() != 200 || err != nil {
			return fmt.Errorf("Error fetching subreddit '%s'", name)
		}
	}

	return nil
}

func GetMedias(source string, name string, limit int) []Submission {
	var after string
	var response Response
	var url string
	submissions := make([]Submission, 0)

	pterm.Print("\n📝 Collecting files from ", source, " ", pterm.Bold.Sprintf(name), " ")

	for {
		if source == "user" {
			url = createUserUrl(name, after)
		} else {
			url = createSubredditUrl(name, after)
		}

		_, _ = client.R().SetResult(&response).Get(url)

		list := response.Data.Children
		submissions = append(submissions, funk.Map(list, func(c Children) Submission {
			return c.Data
		}).([]Submission)...)

		// Getting the last "after"
		if response.Data.After == after {
			after = ""
		} else {
			after = response.Data.After
		}

		pterm.Print(".")

		if len(list) == 0 || len(submissions) >= limit || after == "" {
			break
		}
	}

	submissions = getUniqueSubmissions(submissions)
	if len(submissions) > limit {
		submissions = submissions[:limit]
	}

	pterm.Printf(" %d/%d unique posts found\n\n", len(submissions), limit)
	return submissions
}

// region - Private functions

func createUserUrl(user string, after string) string {
	return fmt.Sprintf("https://www.reddit.com/user/%s/submitted.json?limit=100&sort=new&after=%s&raw_json=1",
		user, after)
}

func createSubredditUrl(subreddit string, after string) string {
	return fmt.Sprintf("https://www.reddit.com/r/%s/hot.json?limit=100&after=%s&raw_json=1", subreddit, after)
}

func getUniqueSubmissions(submissions []Submission) []Submission {
	uniqueMap := make(map[string]Submission)
	uniqueArr := make([]Submission, 0)

	for _, elem := range submissions {
		if _, exists := uniqueMap[elem.Url]; !exists {
			uniqueMap[elem.Url] = elem
			uniqueArr = append(uniqueArr, elem)
		}
	}

	return uniqueArr
}

// endregion
