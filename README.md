# Liberty Tools for IntelliJ IDEA

![JetBrains plugins](https://img.shields.io/jetbrains/plugin/v/14856-liberty-tools.svg?style=for-the-badge)
![Downloads](https://img.shields.io/jetbrains/plugin/d/14856-liberty-tools?style=for-the-badge&)
[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?style=for-the-badge&label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)

An IntelliJ IDEA plugin offering features for developing cloud-native Java applications with [Open Liberty](https://openliberty.io/) and [WebSphere Liberty](https://www.ibm.com/products/websphere-liberty). Iterate fast with Liberty dev mode, code with assistance for MicroProfile and Jakarta EE APIs, and easily edit Liberty configuration files.

**Note: This is an early release.**

![Liberty Tools Extension](docs/images/liberty-tool-window-view.png)

This plugin allows you to run your Liberty Maven or Liberty Gradle projects through the Liberty tool window or the IntelliJ "Go to Action" menu. You can start, stop, or interact with [Liberty dev mode](https://openliberty.io/docs/latest/development-mode.html) on all configured [Liberty Maven](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#dev) and [Liberty Gradle](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md) projects in your workspace. Liberty Tools help you quickly and easily edit your application and configuration files by providing language support features for MicroProfile, Jakarta EE, and Liberty configuration and Java files.

## Features
- View supported Liberty projects in the Liberty tool window.
- Start/Stop dev mode.
- Start dev mode with custom parameters.
- Start dev mode in a container.
- Start dev mode with the debugger attached.
- Run tests.
- View test reports.

Liberty Tools for IntelliJ IDEA consumes the [Liberty Config Language Server](https://github.com/OpenLiberty/liberty-language-server) to provide editing assistance for Liberty server configuration files:
- server.xml
- server.env
- bootstrap.properties

For more information, see the [project documentation in GitHub](https://github.com/OpenLiberty/liberty-language-server#liberty-config-language-server).

![Liberty Config Language Server completion](docs/images/LCLS-server-xml-completion.png)

Liberty Tools for IntelliJ IDEA consumes [Eclipse LSP4Jakarta](https://github.com/eclipse/lsp4jakarta), the Language Server for Jakarta EE, to provide editing assistance for Jakarta EE APIs in Java files. For more information, see the [project documentation in GitHub](https://github.com/eclipse/lsp4jakarta#eclipse-lsp4jakarta).

_insert screenshot_

Liberty Tools for IntelliJ IDEA consumes [Eclipse LSP4MP](https://github.com/eclipse/lsp4mp), the Language Server for MicroProfile, to provide editing assistance for Jakarta EE APIs in Java and microprofile-config.properties files. For more information, see the [project documentation in GitHub](https://github.com/eclipse/lsp4mp#eclipse-lsp4mp---language-server-for-microprofile).

Eclipse LSP4Jakarta Snippets            | Eclipse LSP4MP Snippets
:-------------------------:|:-------------------------:
![LSP4Jakarta](docs/images/LSP4Jakarta-rest-completion.png) | ![LSP4MP](docs/images/LSP4MP-rest-completion.png)

## Quick Start

- Install [_Liberty Tools_ from the JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14856-liberty-tools).
- Projects with the Liberty Maven Plugin or Liberty Gradle Plugin configured will appear in the Liberty tool window on
  the sidebar. If not enabled by default, the tool window can be viewed by selecting **View > Tool Windows > Liberty**.
- Select a project in the Liberty tool window to view the available commands.

## Action Commands

| Command                              | Description                                                                                                                                                                                                                                                                                                                                                                                                               |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Start                                | Starts dev mode.                                                                                                                                                                                                                                                                                                                                                                                                          |
| Start…                               | Opens the Run Configurations dialog to customize and start dev mode. Supported parameters can be found in the documentation for the [dev goal of the Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#additional-parameters) and the [libertyDev task of the Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md#command-line-parameters). |
| Start in a container                 | Starts dev mode with the server in a container. The `liberty-maven-plugin` must be version `3.3-M1` or higher. The `liberty-gradle-plugin` must be version `3.1-M1` or higher.                                                                                                                                                                                                                                            
| Stop                                 | Stops dev mode. This command requires dev mode to have been started.                                                                                                                                                                                                                                                                                                                                                                                                          |
| Run tests                            | Runs the unit tests and integration tests that are configured for your project. This command requires dev mode to have been started.                                                                                                                                                                                                                                                                               |
| View integration test report (Maven) | Views the integration test report file if it exists at `/target/site/failsafe-report.html`.                                                                                                                                                                                                                                                                                                                               |
| View unit test report (Maven)        | Views the unit test report file if it exists at `/target/site/surefire-report.html`.                                                                                                                                                                                                                                                                                                                                      |
| View test report (Gradle)            | Opens the test report file if it exists at the default location `build/reports/tests/test/index.html`. This action command is only available to Gradle projects. Gradle projects only have a single action command for test result reporting.                                                                                                                                                                             |

## Requirements

**IntelliJ IDEA**: Starting with the Liberty Tools for IntelliJ IDEA 0.0.8 release, IntelliJ IDEA version 2022.2 is required. Liberty Tools for IntelliJ IDEA is compatible with the Community Edition of IntelliJ IDEA.

## Contributing

See the [DEVELOPING](DEVELOPING.md) and [CONTRIBUTING](CONTRIBUTING.md) documents for more details.

## Issues

Please report bugs, issues and feature requests by creating
a [GitHub issue](https://github.com/OpenLiberty/liberty-tools-intellij/issues).