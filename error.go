package main

import (
	"fmt"
	"github.com/pterm/pterm"
)

func PrintError(message string, a ...interface{}) {
	format := fmt.Sprintf(message, a...)
	pterm.Printf(pterm.Red("\n🧨 %s\n"), format)
}

func SprintError(message string, a ...interface{}) string {
	format := fmt.Sprintf(message, a...)
	return pterm.Sprintf(pterm.Red("\n🧨 %s\n"), format)
}
