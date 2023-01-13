/*******************************************************************************
 * Copyright (c) 2022, 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package io.openliberty.tools.intellij.lsp4mp.lsp4ij;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.services.LanguageServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LanguageServersRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageServersRegistry.class);

    public abstract static class LanguageServerDefinition {
        public final @Nonnull String id;
        public final @Nonnull String label;
        public final boolean isSingleton;
        public final @Nonnull Map<Language, String> languageIdMappings;
        public final Map<Language, String> languageFilePatternMappings;

        public LanguageServerDefinition(@Nonnull String id, @Nonnull String label, boolean isSingleton) {
            this.id = id;
            this.label = label;
            this.isSingleton = isSingleton;
            this.languageIdMappings = new ConcurrentHashMap<>();
            this.languageFilePatternMappings = new ConcurrentHashMap<>();
        }

        public void registerAssociation(@Nonnull Language language, @Nonnull String languageId, String filePattern) {
            this.languageIdMappings.put(language, languageId);
            if (filePattern != null) {
                this.languageFilePatternMappings.put(language, filePattern);
            }
        }

        public abstract StreamConnectionProvider createConnectionProvider();

        public LanguageClientImpl createLanguageClient(Project project) {
            return new LanguageClientImpl(project);
        }

        public Class<? extends LanguageServer> getServerInterface() {
            return LanguageServer.class;
        }

    }

    static class ExtensionLanguageServerDefinition extends LanguageServerDefinition {
        private ServerExtensionPointBean extension;

        public ExtensionLanguageServerDefinition(ServerExtensionPointBean element) {
            super(element.id, element.label, element.singleton);
            this.extension = element;
        }

        @Override
        public StreamConnectionProvider createConnectionProvider() {
            try {
                return extension.getInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Exception occurred while creating an instance of the stream connection provider", e); //$NON-NLS-1$
            }
        }

        @Override
        public LanguageClientImpl createLanguageClient(Project project) {
            String clientImpl = extension.clientImpl;
            if (clientImpl != null && !clientImpl.isEmpty()) {
                try {
                    return (LanguageClientImpl) project.instantiateClass(extension.getClientImpl(),
                            extension.getPluginDescriptor().getPluginId());
                } catch (ClassNotFoundException e) {
                    LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
            return super.createLanguageClient(project);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends LanguageServer> getServerInterface() {
            String serverInterface = extension.serverInterface;
            if (serverInterface != null && !serverInterface.isEmpty()) {
                    try {
                        return (Class<? extends LanguageServer>) (Class<?>)extension.getServerInterface();
                    } catch (ClassNotFoundException exception) {
                        LOGGER.warn(exception.getLocalizedMessage(), exception);
                    }
                }
            return super.getServerInterface();
            }
        }

    private static LanguageServersRegistry INSTANCE = null;
    public static LanguageServersRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LanguageServersRegistry();
        }
        return INSTANCE;
    }

    private List<ContentTypeToLanguageServerDefinition> connections = new ArrayList<>();

    private LanguageServersRegistry() {
        initialize();
    }

    private void initialize() {
        LOGGER.info("***** Initializing Language Servers *****");
        Map<String, LanguageServerDefinition> servers = new HashMap<>();
        List<LanguageMapping> languageMappings = new ArrayList<>();
        for (ServerExtensionPointBean server : ServerExtensionPointBean.EP_NAME.getExtensions()) {
            if (server.id != null && !server.id.isEmpty()) {
                servers.put(server.id, new ExtensionLanguageServerDefinition(server));
            }
        }
        for (LanguageMappingExtensionPointBean extension : LanguageMappingExtensionPointBean.EP_NAME.getExtensions()) {
            Language language = Language.findLanguageByID(extension.language);
            if (language != null) {
                languageMappings.add(new LanguageMapping(language, extension.id, extension.serverId, extension.filePattern));
            }
        }

        for (LanguageMapping mapping : languageMappings) {
            LanguageServerDefinition lsDefinition = servers.get(mapping.languageId);
            if (lsDefinition != null) {
                LOGGER.info("Registering language server '" + lsDefinition.id + "' with language: " + mapping.language.getID() + " and file pattern: " + lsDefinition.languageFilePatternMappings);
                registerAssociation(mapping.language, lsDefinition, mapping.languageId, mapping.filePattern);
            } else {
                LOGGER.warn("Language server '" + mapping.id + "' not available"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        LOGGER.info("**********");
    }

    /**
     * @param contentType
     * @return the {@link LanguageServerDefinition}s <strong>directly</strong> associated to the given content-type.
     * This does <strong>not</strong> include the one that match transitively as per content-type hierarchy
     */
    List<ContentTypeToLanguageServerDefinition> findProviderFor(final @NonNull Language contentType) {
        return connections.stream()
                .filter(entry -> contentType.isKindOf(entry.getKey()))
                .collect(Collectors.toList());
    }


    public void registerAssociation(@Nonnull Language language,
                                    @Nonnull LanguageServerDefinition serverDefinition, @Nullable String languageId, @Nullable String filePattern) {
        if (languageId != null) {
            serverDefinition.registerAssociation(language, languageId, filePattern);
        }

        connections.add(new ContentTypeToLanguageServerDefinition(language, serverDefinition));
    }

    public List<ContentTypeToLanguageServerDefinition> getContentTypeToLSPExtensions() {
        return this.connections.stream().filter(mapping -> mapping.getValue() instanceof ExtensionLanguageServerDefinition).collect(Collectors.toList());
    }

    public @Nullable LanguageServerDefinition getDefinition(@NonNull String languageServerId) {
        for (ContentTypeToLanguageServerDefinition mapping : this.connections) {
            if (mapping.getValue().id.equals(languageServerId)) {
                return mapping.getValue();
            }
        }
        return null;
    }

    /**
     * internal class to capture content-type mappings for language servers
     */
    private static class LanguageMapping {

        @Nonnull public final String id;
        @Nonnull public final Language language;
        @Nullable public final String languageId;
        @Nullable public final String filePattern;

        public LanguageMapping(@Nonnull Language language, @Nonnull String id, @Nullable String languageId, @Nullable String filePattern) {
            this.language = language;
            this.id = id;
            this.languageId = languageId;
            this.filePattern = filePattern;
        }

    }

    /**
     * @param file
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull VirtualFile file, @NonNull LanguageServerDefinition serverDefinition,
                           Project project) {
        return getAvailableLSFor(file, project).contains(serverDefinition);
    }

    /**
     * @param document
     * @param serverDefinition
     * @return whether the given serverDefinition is suitable for the file
     */
    public boolean matches(@Nonnull Document document, @Nonnull LanguageServerDefinition serverDefinition,
                           Project project) {
        return getAvailableLSFor(document, project).contains(serverDefinition);
    }

    private Set<LanguageServerDefinition> getAvailableLSFor(Document document, Project project) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return getAvailableLSFor(file, project);
    }

    private Set<LanguageServerDefinition> getAvailableLSFor(VirtualFile file, Project project) {
        Language language = LSPIJUtils.getFileLanguage(file, project);
        Set<LanguageServerDefinition> res = new HashSet<>();
        if (language != null) {
            for (ContentTypeToLanguageServerDefinition mapping : this.connections) {
                if (language.isKindOf(mapping.getKey())) {
                    LanguageServerDefinition lsDef = mapping.getValue();
                    if (!lsDef.languageFilePatternMappings.isEmpty() && lsDef.languageFilePatternMappings.containsKey(language)) {
                        // check if document matches file pattern
                        Path path = Paths.get(file.getCanonicalPath());
                        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + lsDef.languageFilePatternMappings);
                        if (matcher.matches(path)) {
                            LOGGER.info("Available language server: " + mapping.getValue().id + " for file: " + file);
                            res.add(mapping.getValue());
                        }
                    } else {
                        LOGGER.info("Available language server: " + mapping.getValue().id + " for file: " + file);
                        res.add(mapping.getValue());
                    }
                }
            }
        }
        return res;
    }
}

