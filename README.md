# Mini Doodle

## Disclaimers

### Background

* For the last 4 years I have been using mostly Kotlin, so my Java might be rusty.

### Agentic AI usage

* For the past year or so I've been actively using AI agents at work (actively encouraged to). For this task I am not using any.

## Design decisions and thought process

### Codebase
* Although it won't be a huge codebase, I grew really sick of looking at a bunch of unrelated services in a single folder, so using package-by-feature approach.

### Approach
* For a long time now I've been liking using Event Driven approaches (even Event Sourcing), but it fits better in a more distributed environment and requires a bit more care than a small coding challenge should, so we can instead talk about it (if I get to that point) 
* For the outlined scope (hundreds of users, thousands of slots) plain Tomcat with virtual threads will suffice.

### First step

* Although it's probably going to be buried in other commits - as a first step I want to create a barebones app and a simplest e2e setup with the said app and it's dependencies (for now only Postgres) to be available via docker-compose.
* Already wiring most of Gradle dependencies albeit not using them yet but that will for sure be used later (testcontainers and such)
* Using layered JAR from the get go.

## Running the app