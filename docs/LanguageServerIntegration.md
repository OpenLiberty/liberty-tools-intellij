# Language Server Integration

Liberty Tools for IntelliJ currently acts as a client for the following language servers
- [Eclipse LSP4MP - Langauge Server for MicroProfile](https://github.com/eclipse/lsp4mp)
- [LemMinX - XML Language Server](https://github.com/eclipse/lemminx)
- [Liberty Language Server](https://github.com/OpenLiberty/liberty-language-server)
    - `lemminx-liberty` component is an extension to LemMinX delivering support for `server.xml` files
    - `liberty-ls` component delivers support for `bootstrap.properties` and `server.env` files

## Building 
1. Clone and build the [Liberty Language Server](https://github.com/OpenLiberty/liberty-language-server) to install the corresponding jars to your local Maven repository.
`cd lemminx-liberty && mvn clean install`
`cd liberty-ls && mvn clean install`
2. In the Liberty Tools for IntelliJ repo, run `./gradlew runIde`. This should copy the required language server jars to the `build/server/server/` directory. Each language server is started with the corresponding `Server` and `LanguageClient` class. See `com.langserver.devtools.intellij.lsp4mp.lsp.MicroProfileServer` and `com.langserver.devtools.intellij.lsp4mp.lsp.MicroProfileLanguageClient` for reference. See also `src/main/resources/META-INF/lsp.xml`.

## Debugging LemMinX Language Server
To debug the LemMinX Language Server in IntelliJ, start Liberty Tools for IntelliJ by creating an IntelliJ debug configuration for `./gradlew runIde command`, then:
1. Create a new debug configuration: _Remote JVM Debug_ --> specify _localhost_, port _1054_ and command line arguments `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1054`
2. In `com.langserver.devtools.intellij.liberty.lsp.LibertyXmlServer.LibertyXmlServer()` replace the line ` params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1054,quiet=y");` with  `params.add("-agentlib:jdwp=transport=dt_socket,server=y,address=1054");`
3. Start the debug configuration created in step 1, you should be able to step through the LemMinX LS code now with the IntelliJ debugger