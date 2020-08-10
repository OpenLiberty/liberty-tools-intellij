# Open Liberty Tools for IntelliJ

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)


An [Open Liberty](https://openliberty.io/) extension for IntelliJ. The extension will detect your Liberty Maven or Liberty 
Gradle project if it detects the `io.openliberty.tools:liberty-maven-plugin` in the `pom.xml` or 
`io.openliberty.tools:liberty-gradle-plugin` in the `build.gradle`.  Through the Liberty Dev Dashboard, you can start,
stop, or interact with Liberty dev mode on all available 
[Liberty Maven](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#dev) 
and [Liberty Gradle](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md) projects in your workspace.

Note that this extension requires the [Integrated Terminal plugin](https://plugins.jetbrains.com/plugin/13123-terminal) to be enabled.

## Quick Start

- Search for _Open Liberty Tools_ in IntelliJ **Settings > Plugins > Marketplace** and click the Install button
- Open Liberty supported projects will appear in the Liberty Dev Dashboard on the side bar
- Click a project in the Liberty Dev Dashboard to view the available commands

## Features

- View supported `liberty-maven-plugin`(version `3.1` or higher) or `liberty-gradle-plugin`(version `3.0` or higher) projects in the workspace
- Start/Stop dev mode
- Start dev mode with custom parameters
- Run tests
- View unit and integration test reports

## Commands

| Command                      | Description                                                                                                                                                                                                                                                                                                                  |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Start                        | Starts dev mode.                                                                                                                                                                                                                                                                                                             |
| Start…​                      | Starts dev mode with custom parameters. Supported parameters can be found in the documentation for the [dev goal of the Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#additional-parameters) and the [libertyDev task of the Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md#command-line-parameters). |
| Stop                         | Stops dev mode.                                                                                                                                                                                                                                                                                                              |
| Run tests                    | Runs the unit tests and integration tests that are configured for your project. This command requires dev mode to be already started.                                                                                                                                                                                        |
| View integration test report | Views the integration test report file.                                                                                                                                                                                                                                                                                      |
| View unit test report        | Views the unit test report file.                                                                                                                                                                                                                                                                                             |

**Note:** Gradle projects only have a single `View test report` command.

## Contributing

Contributions to the Open Liberty Tools extension are welcome!

Our [CONTRIBUTING](CONTRIBUTING.md) document contains details for submitting pull requests.

Developing this extension using the built-in [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin/).

1. Clone this repository: `git clone git@github.com:OpenLiberty/open-liberty-tools-intellij.git`
2. Import this repository as a gradle project in IntelliJ IDEA
3. Run `./gradlew buildPlugin` to build a `.zip` that can be imported as gradle plugin or run the following Gradle task to build and run an IntelliJ instance:
`./gradlew runIde`

## Issues

Please report bugs, issues and feature requests by creating a [GitHub issue](https://github.com/OpenLiberty/open-liberty-tools-intellij/issues)