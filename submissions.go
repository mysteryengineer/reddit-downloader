package main

import (
	"fmt"
	"github.com/go-resty/resty/v2"
	"github.com/pterm/pterm"
	"github.com/thoas/go-funk"
	"math"
)

var client = resty.New()

func GetUserMedias(name string, limit int) []Submission {
	var after string
	var response Response
	submissions := make([]Submission, 0)
	counter := 0

	pterm.Print("\n📝 Collecting files from user ", pterm.Bold.Sprintf(name), " ")

	for {
		url := createUserUrl(name, after)
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

		counter++
		pterm.Print(".")

		if len(list) == 0 || counter >= int(math.Ceil(float64(limit)/100)) || after == "" {
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
