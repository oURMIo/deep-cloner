# Deep Cloner

## Overview

The Deep Cloner project is designed to address the problem described in `Task.md`.
The project is implemented in Kotlin 21 and uses Gradle for build automation.
<p>The primary functionality of this project is to create deep copies of objects, ensuring that all nested objects are also cloned.</p>

## Project Structure

```text
deep-cloner/
│
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/
│   │   │       └── home/
│   │   │           └── playground/
│   │   │               ├── Main.kt
│   │   │               ...
│   └── test/
│       └── kotlin/
│           └── com/
│               └── home/
│                   └── playground/
│                       └── CopyUtilsTest.kt
│
├── build.gradle.kts
├── settings.gradle.kts
├── Task.md
└── README.md
```

## Getting Started

Prerequisites:

* Kotlin 21
* Gradle

## Setup

### Clone the repository

```shell
git clone https://github.com/oURMIo/deep-cloner.git
cd deep-cloner
```

Build the project

```shell
./gradlew build
```

### Running the Application

To run the application, use the main method in com.home.playground.MainKt:

```shell
./gradlew run
```

Or, if you are using an IDE, you can run the main method directly from `com.home.playground.MainKt`.

### Running the Tests

To run the tests, use:

```shell
./gradlew test
```

The tests are located in `com.home.playground.CopyUtilsTest`.
