# Developing Liberty Tools for IntelliJ IDEA

> Note: Starting with the [0.0.8 early release](https://github.com/OpenLiberty/liberty-tools-intellij/releases/tag/0.0.8), Java 17 (bundled with IntelliJ IDEA version 2022.2+) and a minimum version of IntelliJ IDEA version 2022.2 are required to run Liberty Tools for IntelliJ IDEA.

- [Building Liberty Tools for IntelliJ IDEA](#building-liberty-tools-for-intellij-idea)
- [Language Servers](#language-servers)
  - [Build Liberty Config Language Server locally](#build-liberty-config-language-server-locally)
    - [Debugging LemMinX langauge server with the Liberty LemMinX extension](#debugging-lemminx-language-server-with-the-liberty-lemminx-extension)
  - [Build Eclipse LSP4Jakarta locally](#build-eclipse-lsp4jakarta-locally)
  - [Build Eclipse LSP4MP locally](#build-eclipse-lsp4mp-locally)
  - [Monitoring language server messages](#monitoring-language-server-messages)
  - [Continuous Integration of LSP4IJ](#continuous-integration-of-lsp4ij)
- [Localization](#localization)
  - [LibertyBundles.properties](#libertybundlesproperties)
  - [Source code](#source-code)

## Building Liberty Tools for IntelliJ IDEA

This extension is built using the [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin/).

1. Clone this repository: `git clone git@github.com:OpenLiberty/liberty-tools-intellij.git`
2. Clone the lsp4ij repository: `git clone git@github.com:MicroShed/lsp4ij.git`
3. Build lsp4ij and save it in your local Maven repository. Run the following command from your local clone of the lsp4ij repository: `./gradlew publishToMavenLocal`.
4. Import this repository as a Gradle project in IntelliJ IDEA 
5. Run `./gradlew runIde --stacktrace`. A new IntelliJ IDEA window will launch with the Liberty Tools plugin installed to it. You can connect the IntelliJ IDEA debugger to this process to debug the plugin.

   OR  

   Run `./gradlew buildPlugin` to build an installable zip in `build/distributions/liberty-tools-intellij-xxx.zip`. You can install this zip in IntelliJ IDEA through **Preferences > Plugins > Gear icon > Install Plugin from Disk...** and select the `liberty-tools-intellij-xxx.zip`.

## Language Servers

Liberty Tools for IntelliJ consumes the [Liberty Config Language Server](https://github.com/OpenLiberty/liberty-language-server), [Eclipse LSP4Jakarta](https://github.com/eclipse/lsp4jakarta), and [Eclipse LSP4MP](https://github.com/eclipse/lsp4mp) projects. The language server JARS are automatically downloaded from Maven Central and the Eclipse repository. The following instructions explain how to build these JARs locally and run them with Liberty Tools for IntelliJ IDEA.

### Build Liberty Config Language Server locally

1. Follow the Liberty Config Language Server [build instructions](https://github.com/OpenLiberty/liberty-language-server/blob/main/DEVELOPING.md#projects) to build the `lemminx-liberty-x.x-SNAPSHOT.jar` and `liberty.ls-x.x-SNAPSHOT.jar`.
2. In the [build.gradle file](build.gradle), update references to the `liberty-langserver-lemminx` and `liberty-langserver` to point to the versions built in the previous step.
3. Until [#173](https://github.com/OpenLiberty/liberty-tools-intellij/issues/173) is fixed, update the jar versions in `io.openliberty.tools.intellij.liberty.lsp.LibertyXmlServer` and `io.openliberty.tools.intellij.liberty.lsp.LibertyConfigLanguageServer`.

#### Debugging LemMinX Language Server
To debug the LemMinX Language Server in IntelliJ, complete the following steps.
1. Start Liberty Tools for IntelliJ by creating an IntelliJ debug configuration for the `./gradlew runIde command`.
2. Create a new debug configuration: _Remote JVM Debug_ --> specify _localhost_, port _1054_ and command line arguments `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1054`
3. In `io.openliberty.tools.intellij.liberty.lsp.LibertyXmlServer.LibertyXmlServer()` replace the line ` params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1054,quiet=y");` with  `params.add("-agentlib:jdwp=transport=dt_socket,server=y,address=1054");`.
4. Start the debug configuration created in step 2. You can now step through the LemMinX LS code now with the IntelliJ debugger.

### Build Eclipse LSP4Jakarta locally

1. Follow the Eclipse LSP4Jakarta [build instructions](https://github.com/eclipse/lsp4jakarta/blob/main/docs/BUILDING.md#building) to build the `org.eclipse.lsp4jakarta.ls-x.x.x-SNAPSHOT-jar-with-dependencies.jar`. You do not need to build the `lsp4jakarta.jdt` or `lsp4jakarta.eclipse` components.
2. In the [build.gradle file](build.gradle), update references to the `org.eclipse.lsp4jakarta.ls` to point to the version built in the previous step.

### Build Eclipse LSP4MP locally

1. Follow the Eclipse LSP4MP [build instructions](https://github.com/eclipse/lsp4mp#getting-started) to build the `org.eclipse.lsp4mp.ls-x.x.x-SNAPSHOT-jar-with-dependencies.jar`. You do not need to build the `lsp4mp.jdt` component.
2. In the [build.gradle file](build.gradle), update references to the `org.eclipse.lsp4mp.ls` to point to the version built in the previous step.

### Monitoring language server messages

1. Click the **Language Servers** tool window in the IntelliJ IDE to show the **LSP Consoles**.
2. Select the language server you wish to monitor and then select **Trace:**  **messages** or **verbose**.
3. Messages to and from the language server will appear in the console, so you can verify the messages sent in response to user actions in the IDE.

For more information, see the [LSP console user guide](https://github.com/redhat-developer/lsp4ij/blob/main/docs/UserGuide.md#lsp-console)

### Continuous Integration of LSP4IJ

For details on the Continuous Integration (CI) setup for LSP4IJ integration, refer to the [LSP4IJ Continuous Integration documentation.](docs/LSP4IJ-Continuous-Integration.md) 

## Localization

### LibertyBundles.properties
Add localized strings in `src/main/resources/messages/LibertyBundles_{locale}.properties`. The default message file is `LibertyBundles.properties`.

### Source code

1. Add new messages in `src/main/resources/messages/LibertyBunldes_{locale}.properties` file. If message has parameters, use curly brackets to enclose them: `{0}`, `{1}`...

2. Add the following import statement in your source code:

   ```java
   import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
   ```

3. Call method `LocalizedResourceUtil.getMessage` to return localized message.

   Example without parameters:
   ```java
   String message = LocalizedResourceUtil.getMessage("my.message.key");
   ```
   Example with parameters:
   ```java
   String message = LocalizedResourceUtil.getMessage("my.message.key.with.params", param1, param2);
      ```
### Test Videos
To record videos for all tests, not just the failed ones, you can create a `video.properties` file in the `src/test/resources` directory and add `video.save.mode=ALL` to that file.