# Liberty Tools for IntelliJ IDEA User Guide

Detailed instructions on how to import and configure your Liberty project to make use of the Liberty Tools for IntelliJ IDEA plugin.

1. [Before you begin](#before-you-begin)
    - [Software requirements](#software-requirements)
    - [Application requirements](#application-requirements)
    - [Settings](#settings)
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
9. [Configuring a Liberty server](#configuring-a-liberty-server)
10. [Developing with Jakarta EE and MicroProfile APIs](#developing-with-jakarta-ee-and-microprofile-apis)

## Before you begin
### Software requirements
- **IntelliJ IDEA**: Starting with the Liberty Tools for IntelliJ IDEA 0.0.8 release, IntelliJ IDEA version 2022.2 is required. Liberty Tools for IntelliJ IDEA is compatible with the Community Edition of IntelliJ IDEA.
- [Liberty Tools for IntelliJ IDEA plugin](plugins.jetbrains.com/plugin/14856-open-liberty-tools)

### Application requirements
- Define a Liberty `server.xml` configuration file at location `src/main/liberty/config`.
- Configure the [Liberty Maven Plugin](https://github.com/OpenLiberty/ci.maven#configuration) or [Liberty Gradle Plugin](https://github.com/OpenLiberty/ci.gradle#adding-the-plugin-to-the-build-script). We recommend using newer versions of the plugins as several important bug fixes have been included in recent versions.

  Recommended minimum versions of:

    - Liberty Maven Plugin -> 3.7.1

    - Liberty Gradle Plugin -> 3.5.1

### Settings

Liberty Tools for IntelliJ IDEA will honour the:
- Maven home path set in the **Preferences > Build, Execution, Deployment > Build Tools > Maven** window when running Liberty dev mode on Maven projects.
- Gradle user home set in the **Preferences > Build, Execution, Deployment > Build Tools > Gradle** window when running Liberty dev mode on Gradle projects.

## Opening the Liberty tool window
Select **View > Tool Windows > Liberty**.

Projects that are already properly configured to run on Liberty and use Liberty dev mode are automatically added to the Liberty tool window when it opens.

If you add new projects or make changes, and you need the tool window to be refreshed, use the refresh icon in the Liberty tool window toolbar.

_insert screenshot of refresh button highlighted_

## Running your application on Liberty using dev mode

There are three options ([Start](#start), [Start...](#start-with-configuration), [Start in container](#start-in-container)) for starting your Liberty application in dev mode by using the menu actions provided through the Liberty tool window view or the "Go to Action" IntelliJ IDEA menu.

### Using the Liberty tool window

The Liberty tool window provides a context menu to the Liberty projects listed. Users can choose the menu contents to perform a series of operations aimed to speed up application development.

![Liberty tool window](images/liberty-tool-window-view.png)

### Using the "Go to Action" IntelliJ IDEA menu

The Liberty Tools plugin provides a set of actions to the "Go to Action" IntelliJ IDEA menu. The "Go to Action" IntelliJ IDEA menu can be accessed with the default shortcuts "Shift + Shift" then Actions tab, or "Ctl/Cmd + Shift + A".

![Liberty actions in "Go to Action" menu](images/liberty-go-to-action-view.png)

### Start

If you want to start your application in dev mode, you can either right-click on the application listed in the Liberty tool window, and click on the `Start` action or select the `Liberty: Start` action in the "Go to Action" IntelliJ IDEA menu.

A new terminal tab will open to the run the application in dev mode.

![Liberty Start action](images/liberty-start.png)

### Start with configuration

If you want to start your application in dev mode with custom configuration, you can either right-click on the application listed in the Liberty tool window, and click on the `Start...` action or select the `Liberty: Start...` action in the "Go to Action" IntelliJ IDEA menu. The action opens a Liberty Run/Debug Configuration dialog. You can specify parameters for the [Liberty Maven dev goal](https://github.com/OpenLiberty/ci.maven/blob/main/docs/dev.md#additional-parameters) or [Liberty Gradle dev task](https://github.com/OpenLiberty/ci.gradle/blob/main/docs/libertyDev.md#command-line-parameters).

![Liberty Start... action](images/liberty-start-with-config.png)

Once you are done with customizing the configuration, click `Ok`.

A new terminal tab will open to run the application in dev mode.

![Liberty Start... action running in terminal](images/liberty-start-with-config-terminal.png)

Note that the configuration shown in the Run/Debug Configurations dialog is created and displayed automatically if one is not already associated with the project. If a single configuration is already associated with the project, that configuration is displayed. If multiple configurations are already associated with the project, the highlighted/last ran configuration is displayed.

### Start in container

If you want to make use of dev mode for containers, you can either right-click on the application listed in the Liberty tool window, and click on the `Start in container` action or select the `Liberty: Start in container` action in the "Go to Action" IntelliJ IDEA menu.

For more information on dev mode for containers, check out the [Liberty Maven devc goal](https://github.com/OpenLiberty/ci.maven/blob/main/docs/dev.md#devc-container-mode) or the [Liberty Gradle libertyDevc task](https://github.com/OpenLiberty/ci.gradle/blob/main/docs/libertyDev.md#libertydevc-task-container-mode).

![Liberty Start in container action](images/liberty-start-in-container.png)

## Running your application's tests

Once your application is running on Liberty using dev mode, you can easily run the tests provided by your application.

To do this, you can either right-click on the application listed in the Liberty tool window, and click on the `Run tests` action or select the `Liberty: Run tests` action in the "Go to Action" IntelliJ IDEA menu.

The tests are run in the corresponding terminal.

![Liberty run tests action](images/liberty-run-tests-action.png)

## Viewing your application's test reports

Once you are done running your application's tests, you can access the produced test reports. Note that test reports are opened in your external default browser.

### Maven built application

To view the integration test report for Maven built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View integration test report` action or select the `Liberty: View integration test report` action in the "Go to Action" IntelliJ IDEA menu.

Note that this action will look for the integration test report at the default location `/target/site/failsafe-report.html`.

To view the unit test report for Maven built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View unit test report` action or select the `Liberty: View unit test report` action in the "Go to Action" IntelliJ IDEA menu.

Note that this action will look for the unit test report at the default location `/target/site/surefire-report.html`.

### Gradle built application

To view the test report for Gradle built applications, you can either right-click on the application listed in the Liberty tool window, and click on the `View test report` action or select the `Liberty: View test report` action in the "Go to Action" IntelliJ IDEA menu.

Note that this action will look for the test report at the default location `build/reports/tests/test/index.html`.

## Stopping your application

To stop your application, you can either right-click on the application listed in the Liberty tool window, and click on the `Stop` action or select the `Liberty: Stop` action in the "Go to Action" IntelliJ IDEA menu.

![Liberty stop action](images/liberty-stop-action.png)

## Debugging your application

To debug your application, you can start dev mode with the debugger automatically attached to the Liberty server JVM running your application.

To start dev mode with the debugger attached, first create or select a Liberty Run/Debug Configuration through the IntelliJ Run/Debug Configuration menu. Once you select the Liberty Run/Debug Configuration, select the `Debug` action next to the Run/Debug Configuration menu.

A new terminal tab will open to the run the application in dev mode and the debugger will wait to attach.

![Liberty debug action](images/liberty-debug.png)

The debug dialog will pop up to confirm your Debug Configuration. Select "Debug".

![Liberty debug confirm configuration](images/liberty-debug-confirm-config.png)

Once the server starts, the IntelliJ IDEA debugger will attach and switch to the debug perspective. You can now set breakpoints and debug your code as usual.

![Liberty debugger attached](images/liberty-debugger-attached.png)


## Manually adding your Liberty project to the tool window

To manually add your Liberty project to the Liberty tool window, select the `Liberty: Add project to the tool window` action in the "Go to Action" IntelliJ IDEA menu. 

![Liberty add project to tool window](images/liberty-add-project-to-tool-window.png)

You will be prompted with a list of projects that are not already displayed in the Liberty tool window.

![Liberty add project to tool window selection](images/liberty-add-project-to-tool-window-selection.png)

You can remove manually added Liberty projects from the Liberty tool window by selecting the `Liberty: Remove project from the tool window` action in the "Go to Action" IntelliJ IDEA menu.

## Configuring a Liberty server

Liberty configuration assistance is offered through the Liberty Config Language Server. For more information, see the [project documentation in GitHub](https://github.com/OpenLiberty/liberty-language-server#liberty-config-language-server).

1. Start the project in dev mode, using one of the Liberty tool window start commands above. This will install the Liberty features required for your application and allow generation of a corresponding server.xml schema file.
2. Open your server.xml file. Proceed to use Liberty specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.
3. Open your bootstrap.properties or server.env file. Proceed to use Liberty specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.

![LCLS server.xml completion](images/LCLS-server-xml-completion.png)

## Developing with Jakarta EE and MicroProfile APIs

Editing assistance for configuration and application files for Jakarta EE and MicroProfile APIs is provided through the following language server projects, which this project consumes. For more information, see the documentation for these projects: 
- Jakarta EE APIs in Java files:  [Eclipse LSP4Jakarta](https://github.com/eclipse/lsp4jakarta#eclipse-lsp4jakarta), the Language Server for Jakarta EE.
- MicroProfile APIs in microprofile-config.properties and Java files: [Eclipse LSP4MP](https://github.com/eclipse/lsp4mp#eclipse-lsp4mp---language-server-for-microprofile), the Language Server for MicroProfile.

Open a Java or microprofile-config.properties file. Proceed to use Jakarta EE and MicroProfile specific editing support: completion by typing "Ctl/Cmd + Space" at a given point within the document.

![Eclipse LSP4Jakarta RESTful WS completion](images/LSP4Jakarta-rest-completion.png)

![Eclipse LSP4MP microprofile-config.properties completion](images/LSP4MP-mp-properties-completion.png)