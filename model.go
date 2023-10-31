package main

type Media struct {
	Url      string
	FilePath string
}

type Download struct {
	Url       string
	FilePath  string
	Error     error
	IsSuccess bool
	Hash      string
}
