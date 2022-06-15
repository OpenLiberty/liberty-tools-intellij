package io.openliberty.tools.intellij.ls;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

public class Lsp4mpPreloadingActivity extends PreloadingActivity {
    final private Logger LOG = Logger.getInstance(Lsp4mpPreloadingActivity.class);

    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        LOG.warn("Loading LSP4MP"); // logging as warning will result in showing up when runIde is executed (info messages are not currently showing up)

        // change jar path to your lsp4mp uber jar
        String[] command = new String[]{"java","-jar","/Users/kathrynkodama/devex/editorTooling/open-liberty-tools-intellij/src/main/resources/org.eclipse.lsp4mp.ls-0.4.0-uber.jar"};
        IntellijLanguageClient.addServerDefinition(new RawCommandServerDefinition("java,microprofile-config.properties", command));
        LOG.warn("Added LSP4MP server definition");
    }
}
