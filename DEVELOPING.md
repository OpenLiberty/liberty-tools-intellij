# Developing Liberty Tools for IntelliJ IDEA
- [Build Liberty Tools for IntelliJ IDEA](#build-liberty-tools-for-intellij-idea)
- [Language Servers](#language-servers)
  - [Build Liberty Config Language Server locally](#build-liberty-config-language-server-locally)
  - [Build Eclipse LSP4Jakarta locally](#build-eclipse-lsp4jakarta-locally)
  - [Build Eclipse LSP4MP locally](#build-eclipse-lsp4mp-locally)
- [Localization](#localization)
  - [LibertyBundles.properties](#libertybundlesproperties)
    - [Source code](#source-code)

## Build Liberty Tools for IntelliJ IDEA

This extension is built using the [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin/).

1. Clone this repository: `git clone git@github.com:OpenLiberty/liberty-tools-intellij.git`
2. Import this repository as a Gradle project in IntelliJ IDEA
3. Run `./gradlew runIde --stacktrace`. A new IntelliJ IDEA window will launch with the Liberty Tools plugin installed to it. You can connect the IntelliJ IDEA debugger to this process to debug the plugin.

   OR  

   Run `./gradlew buildPlugin` to build an installable zip in `build/distributions/liberty-tools-intellij-xxx.zip`. You can install this zip in IntelliJ IDEA through **Preferences > Plugins > Gear icon > Install Plugin from Disk...** and select the `liberty-tools-intellij-xxx.zip`.

## Language Servers

Liberty Tools for IntelliJ consumes the Liberty Config Language Server, Eclipse LSP4Jakarta, and Eclipse LSP4MP projects.

### Build Liberty Config Language Server locally
_TODO_

#### Debugging LemMinX Language Server
To debug the LemMinX Language Server in IntelliJ, start Liberty Tools for IntelliJ by creating an IntelliJ debug configuration for `./gradlew runIde command`, then:
1. Create a new debug configuration: _Remote JVM Debug_ --> specify _localhost_, port _1054_ and command line arguments `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1054`
2. In `io.openliberty.tools.intellij.liberty.lsp.LibertyXmlServer.LibertyXmlServer()` replace the line ` params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1054,quiet=y");` with  `params.add("-agentlib:jdwp=transport=dt_socket,server=y,address=1054");`
3. Start the debug configuration created in step 1, you should be able to step through the LemMinX LS code now with the IntelliJ debugger 

### Build Eclipse LSP4Jakarta locally
_TODO_
### Build Eclipse LSP4MP locally
_TODO_
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