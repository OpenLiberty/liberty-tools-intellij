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

