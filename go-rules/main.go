package main

import "fmt"

var count int

func inc(maxCount int, done chan string) {
	for i := 0; i < maxCount; i++ {
		count = count + 1
	}		
	
	done <- "Finished"
}

func main() {
	done := make(chan string)
	for routine := 0; routine <= 4; routine++ {
		go inc(10000, done)
		fmt.Println("count value: ", count)
		fmt.Println(<-done)
	}
}