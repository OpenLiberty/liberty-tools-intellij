# Open Liberty Tools for IntelliJ

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg?label=license&logo=eclipse)](https://www.eclipse.org/legal/epl-2.0/)


An [Open Liberty](https://openliberty.io/) extension for IntelliJ. The extension will detect your Liberty Maven or Liberty 
Gradle project if it detects the `io.openliberty.tools:liberty-maven-plugin` in the `pom.xml` or 
`io.openliberty.tools:liberty-gradle-plugin` in the `build.gradle`.  Through the Liberty Dev Dashboard, you can start,
stop, or interact with Liberty dev mode on all available 
[Liberty Maven](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md#dev) 
and [Liberty Gradle](https://github.com/OpenLiberty/ci.gradle/blob/master/docs/libertyDev.md) projects in your workspace.

Note that this extension requires the [Integrated Terminal plugin](https://plugins.jetbrains.com/plugin/13123-terminal) to be enabled.


## Developing
Developing this extension using the built-in [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin/).

1. Clone this repository: `git clone git@github.com:OpenLiberty/open-liberty-tools-intellij.git`
2. Import this repository as a gradle project in IntelliJ IDEA
3. Run `./gradlew buildPlugin` to build a `.zip` that can be imported as gradle plugin or execute the following Gradle task to build and execute an IntelliJ instance:
`./gradlew runIde`