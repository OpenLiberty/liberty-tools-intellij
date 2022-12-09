# Getting Started

Detailed instructions on how to import and configure your Liberty project to make use of the Liberty Tools for IntelliJ IDEA plugin.

1. [Before you begin](#before-you-begin)
   - [Software requirements](#software-requirements)
   - [Application requirements](#application-requirements)
   - [Avoid trouble](#avoid-trouble)
2. [Opening the Liberty tool window](#opening-the-liberty-tool-window)
3. [Running your application on Liberty using dev mode](#running-your-application-on-liberty-using-dev-mode)
   - [Using the Liberty tool window](#using-the-liberty-tool-window)
   - [Using the "Go to Action" IntelliJ IDEA menu](#using-the-go-to-action-intellij-idea-menu)
   - [Start](#start)
   - [Start with configuration](#start-with-configuration)
   - [Start in container](#start-in-container)
4. [Running your application's tests](#running-your-applications-tests)
5. [Viewing your application's test reports](#viewing-your-applications-test-reports)
6. [Stopping your application](#stopping-your-application)
7. [Debugging your application](#debugging-your-application)
8. [Manually adding your Liberty project to the tool window](#manually-adding-your-liberty-project-to-the-tool-window)
9. [Setting preferences](#setting-preferences)
10. [Configuring a Liberty server](#configuring-a-liberty-server)

11. [Developing with Jakarta EE and MicroProfile APIs](#developing-with-jakarta-ee-and-microprofile-apis)

## Before you begin
### Software requirements
- **IntelliJ IDEA**: Starting with the Liberty Tools for IntelliJ IDEA 0.0.8 release, IntelliJ IDEA version 2022.2 is required. Liberty Tools for IntelliJ IDEA is compatible with the Community Edition of IntelliJ IDEA. 
- [Liberty Tools for IntelliJ IDEA plugin](plugins.jetbrains.com/plugin/14856-open-liberty-tools)

### Application requirements
- Define a Liberty `server.xml` configuration file at location `src/ain/liberty/confgig`.
- Configure the [Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven#configuration) or [Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle#adding-the-plugin-to-the-build-script). We recommend using newer versions of the plugins as several important bug fixes have been included in recent versions.

  Recommended minimum versions of:

  - Liberty Maven Plugin -> 3.7.1

  - Liberty Gradle Plugin -> 3.5.1

## Opening the Liberty tool window
Select **View > Tool Windows > Liberty**. 

_insert screenshot of opening the Liberty tool window_

Projects that are already properly configured to run on Liberty and use Liberty dev mode are automatically added to the Liberty tool window when it opens.

If you add new projects or make changes, and you need the tool window to be refreshed, use the refresh icon in the Liberty tool window toolbar. 

_insert screenshot of refresh button highlighted_

## Running your application on Liberty using dev mode

There are three options ([Start](#start), [Start...](#start-with-configuration), [Start in container](#start-in-container)) for starting your Liberty application in dev mode by using the menu actions provided through the Liberty tool window view or the "Go to Action" IntelliJ IDEA menu.

### Using the Liberty tool window

The Liberty tool window provides a context menu to the Liberty projects listed. Users can choose the menu contents to perform a series of operations aimed to speed up application development.

_insert screenshot from main readme of tool window_

### Using the "Go to Action" IntelliJ IDEA menu

The Liberty Tools plugin provides a set of actions to the "Go to Action" IntelliJ IDEA menu. The "Go to Action" IntelliJ IDEA menu can be accessed with the default shortcuts "Shift + Shift" then Actions tab, or "Ctl/Cmd + Shift + A".

_insert screenshot from the main readme of go to action_

### Start

If you want to start your application in dev mode, you can either right-click on the application listed in the Liberty tool window, and click on the `Start` action or select the `Liberty: Start` action in the "Go to Action" IntelliJ IDEA menu.

A new terminal tab will open to the run the application in dev mode.

_insert screenshot of new terminal window starting dev mode_

### Start with configuration

If you want to start your application in dev mode with custom configuration, you can either right-click on the application listed in the Liberty tool window, and click on the `Start...` action or select the `Liberty: Start...` action in the "Go to Action" IntelliJ IDEA menu. The action opens a Liberty Run/Debug Configuration dialog. You can specify parameters for the [Liberty Maven dev goal](https://github.com/OpenLiberty/ci.maven/blob/main/docs/dev.md#additional-parameters) or [Liberty Gradle dev task](https://github.com/OpenLiberty/ci.gradle/blob/main/docs/libertyDev.md#command-line-parameters).

_insert start... screenshot_

Once you are done with customizing the configuration, click `Ok`.

A new terminal tab will open to run the application in dev mode.

_insert screenshot of start... running in terminal_

Note that the configuration shown in the Run/Debug Configurations dialog is created and displayed automatically if one is not already associated with the project. If a single configuration is already associated with the project, that configuration is displayed. If multiple configurations are already associated with the project, the highlighted/last ran configuration is displayed.

### Start in container

If you want to make use of dev mode for containers, you can either right-click on the application listed in the Liberty tool window, and click on the `Start in container` action or select the `Liberty: Start in container` action in the "Go to Action" IntelliJ IDEA menu.

For more information on dev mode for containers, check out the [Liberty Maven devc goal](https://github.com/OpenLiberty/ci.maven/blob/main/docs/dev.md#devc-container-mode) or the [Liberty Gradle libertyDevc task](https://github.com/OpenLiberty/ci.gradle/blob/main/docs/libertyDev.md#libertydevc-task-container-mode).

_insert screenshots of start in container_

## Running your application's tests

Once your application is running on Liberty using dev mdoe, you can easily run the tests provided by your application.

To do this, you can either right-click on the application listed in the Liberty tool window, and click on the `Run tests` action or select the `Liberty: Run tests` action in the "Go to Action" IntelliJ IDEA menu.

The tests are run in the corresponding terminal.

_insert screenshot of tests running_

## Viewing your application's test reports

Once you are done running your application's tests, you can access the produced test reports. Note that test reports are opened in your external default browser.

### Maven built application

To view the integration test report for Maven built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View integration test report` action or select the `Liberty: View integration test report` action in the "Go to Action" IntelliJ IDEA menu. 

Note that this action will look for the integration test report at the default location `/target/site/failsafe-report.html`.

_insert screenshot_

To view the unit test report for Maven built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View unit test report` action or select the `Liberty: View unit test report` action in the "Go to Action" IntelliJ IDEA menu.

Note that this action will look for the unit test report at the default location `/target/site/surefire-report.html`.

_insert screenshot_

### Gradle built application

To view the test report for Gradle built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View test report` action or select the `Liberty: View test report` action in the "Go to Action" IntelliJ IDEA menu.

Note that this action will look for the test report at the default location `build/reports/tests/test/index.html`.

_insert screenshot_

## Stopping your application

To stop your application, you can either right-click on the application listed in the Liberty tool window, and click on the `Stop` action or select the `Liberty: Stop` action in the "Go to Action" IntelliJ IDEA menu.

_insert screenshot_

## Debugging your application

To debug your application, you can start dev mode with the debugger automatically attached to the Liberty server JVM running your application. 

To start dev mode with the debugger attached, first create or select a Liberty Run/Debug Configuration through the IntelliJ Run/Debug Configuration menu. Once you select the Liberty Run/Debug Configuration, select the `Debug` action next to the Run/Debug Configuration menu.

A new terminal tab will open to the run the application in dev mode and the debugger will wait to attach.

_insert screenshot_

## Manually adding your Liberty project to the tool window

## Setting preferences

The Liberty Tools for IntelliJ IDEA will honour Maven and Gradle preferences set for your IntelliJ workspace. 

_insert screenshot_

## Configuring a Liberty server

Liberty configuration assistance is offered through the Liberty Config Language Server. For more information, see the [project documentation in GitHub](https://github.com/OpenLiberty/liberty-language-server#liberty-config-language-server).

1. Start the project in dev mode, using one of the Liberty tool window start commands above. This will install the Liberty features required for your application and allow generation of a corresponding server.xml schema file. 
2. Open your server.xml file. Proceed to use Liberty specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.
3. Open your bootstrap.properties or server.env file. Proceed to use Liberty specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.


_insert screenshots_

## Developing with Jakarta EE and MicroProfile APIs

Jakarta EE API assistance is offered through Eclipse LSP4Jakarta, the Language Server for Jakarta EE. For more information, see the [project documentation in GitHub](https://github.com/eclipse/lsp4jakarta#eclipse-lsp4jakarta).

MicroProfile API assistance is offered through Eclipse LSP4MP, the Language Server for MicroProfile. For more information, see the [project documentation in GitHub](https://github.com/eclipse/lsp4mp#eclipse-lsp4mp---language-server-for-microprofile).

1. Start the project in dev mode, using one of the Liberty tool window start commands above. 
2. Open a Java or microprofile-config.properties file. Proceed to use Jakarta EE and MicroProfile specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.