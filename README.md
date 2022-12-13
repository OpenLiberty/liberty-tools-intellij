# Liberty Tools for IntelliJ IDEA

![JetBrains plugins](https://img.shields.io/jetbrains/plugin/v/14856-liberty-tools.svg?style=for-the-badge)
![Downloads](https://img.shields.io/jetbrains/plugin/d/14856-liberty-tools?style=for-the-badge&)
[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?style=for-the-badge&label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)

An IntelliJ IDEA plugin for developing cloud-native Java applications with [Open Liberty](https://openliberty.io/) and [WebSphere Liberty](https://www.ibm.com/products/websphere-liberty). Iterate fast with Liberty dev mode, code with assistance for MicroProfile and Jakarta EE APIs, and easily edit Liberty configuration files.

**Note: This is an early release.**

---
- [Features](#features)
- [Quick Start](#quick-start)
- [Actions](#actions)
- [Requirements](#requirements)
- [Contributing](#contributing)
- [Issues](#issues)

![Liberty Tools Extension](docs/images/liberty-tool-window-view.png)

Use Liberty Tools to run your Liberty Maven or Liberty Gradle projects through the Liberty tool window or the IntelliJ "Go to Action" menu. You can start, stop, or interact with [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html) on all configured [Liberty Maven](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#dev) and [Liberty Gradle](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md) projects in your workspace. Liberty Tools also help you quickly and easily edit your application and configuration files by providing language support features for MicroProfile, Jakarta EE, and Liberty configuration and Java files.

## Features
- View Liberty projects in the Liberty tool window.
- Start/Stop dev mode.
- Start dev mode with custom parameters.
- Start dev mode in a container.
- Start dev mode with the debugger attached.
- Run tests.
- View test reports.
- Code with language assistance in the following configuration and application files:
    - server.xml, server.env, bootstrap.properties Liberty configuration files
    - Jakarta EE 9.x APIs in Java files
    - MicroProfile APIs in microprofile-config.properties and Java files

Feature completion in server.xml files helps you quickly edit your Liberty runtime configuration.

![Liberty Config Language Server completion](docs/images/LCLS-server-xml-completion.png)

Helpful code snippets are provided for Jakarta EE APIs in Java files, such as Jakarta RESTful Web Services :

![LSP4Jakarta completion](docs/images/LSP4Jakarta-rest-completion.png)

Code completion when editing microprofile-config.properties files helps you easily set properties for MicroProfile APIs.

![LSP4MP completion](docs/images/LSP4MP-mp-properties-completion.png)

Editing assistance for configuration and application files is provided through the following language server projects, which this project consumes. For more information, see the documentation for these projects.

- Liberty configuration files: [Liberty Config Language Server](https://github.com/OpenLiberty/liberty-language-server#liberty-config-language-server)
- Jakarta EE APIs in Java files:  [Eclipse LSP4Jakarta](https://github.com/eclipse/lsp4jakarta#eclipse-lsp4jakarta), the Language Server for Jakarta EE.
- MicroProfile APIs in microprofile-config.properties and Java files: [Eclipse LSP4MP](https://github.com/eclipse/lsp4mp#eclipse-lsp4mp---language-server-for-microprofile), the Language Server for MicroProfile.

## Quick Start

- Install [_Liberty Tools_ from the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14856-liberty-tools).
- Projects with the Liberty Maven Plugin or Liberty Gradle Plugin configured will appear in the Liberty tool window on
  the sidebar. If not enabled by default, the tool window can be viewed by selecting **View > Tool Windows > Liberty**.
- Select a project in the Liberty tool window to view the available commands.

For minimum requirements information  and detailed instructions on how to use the Liberty actions, check out the [Liberty Tools for IntelliJ IDEA user guide](docs/user-guide.md) page.

## Actions

The following actions are available when you select a project in the Liberty Tool Window.

| Command                              | Description                                                                                                                                                                                                                                                                                                                                                                                                               |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Start                                | Starts dev mode.                                                                                                                                                                                                                                                                                                                                                                                                          |
| Start…                               | Opens the Run Configurations dialog to customize and start dev mode. Supported parameters can be found in the documentation for the [dev goal of the Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#additional-parameters) and the [libertyDev task of the Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md#command-line-parameters). |
| Start in a container                 | Starts dev mode with Liberty running in a container. The `liberty-maven-plugin` must be version `3.3-M1` or higher. The `liberty-gradle-plugin` must be version `3.1-M1` or higher.                                                                                                                                                                                                                                            
| Stop                                 | Stops dev mode. Liberty must be running in dev mode to use this command.                                                                                                                                                                                                                                                                                                                                                                                                          |
| Run tests                            | Runs the unit tests and integration tests that are configured for your project. Liberty must be running in dev mode to use this command.                                                                                                                                                                                                                               |
| View integration test report (Maven) | Views the integration test report file if it exists at `/target/site/failsafe-report.html`.                                                                                                                                                                                                                                                                                                                               |
| View unit test report (Maven)        | Views the unit test report file if it exists at `/target/site/surefire-report.html`.                                                                                                                                                                                                                                                                                                                                      |
| View test report (Gradle)            | Opens the test report file if it exists at the default location `build/reports/tests/test/index.html`. This action command is only available to Gradle projects. Gradle projects only have a single action command for test result reporting.                                                                                                                                                                             |

## Requirements

Starting with the Liberty Tools for IntelliJ IDEA 0.0.8 release, IntelliJ IDEA version 2022.2 is required. Liberty Tools for IntelliJ IDEA is compatible with the Community Edition of IntelliJ IDEA.

## Contributing

See the [DEVELOPING](DEVELOPING.md) and [CONTRIBUTING](CONTRIBUTING.md) documents for more details.

## Issues

Please report bugs, issues and feature requests by creating
a [GitHub issue](https://github.com/OpenLiberty/liberty-tools-intellij/issues).