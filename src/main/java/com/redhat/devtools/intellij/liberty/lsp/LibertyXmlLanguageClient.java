package com.redhat.devtools.intellij.liberty.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.redhat.devtools.intellij.quarkus.lsp4ij.LanguageClientImpl;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibertyXmlLanguageClient extends LanguageClientImpl implements XMLLanguageClientAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlLanguageClient.class);

    private final MessageBusConnection connection;

    public LibertyXmlLanguageClient(Project project) {
        super(project);
        connection = project.getMessageBus().connect(project);
    }

}
