/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp4ij.psi.core.project;

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PsiMicroProfileProject} manager.
 * 
 * @author Angelo ZERR
 * @see <a href="https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java">https://github.com/redhat-developer/quarkus-ls/blob/master/microprofile.jdt/com.redhat.microprofile.jdt.core/src/main/java/com/redhat/microprofile/jdt/core/project/JDTMicroProfileProjectManager.java</a>
 *
 */
@Service
public final class PsiMicroProfileProjectManager {

	public static PsiMicroProfileProjectManager getInstance(Project project) {
		return ServiceManager.getService(project, PsiMicroProfileProjectManager.class);
	}

	private Project project;

	private final Map<Module, PsiMicroProfileProject> projects;
	private MicroProfileProjectListener microprofileProjectListener;

	private class MicroProfileProjectListener implements ModuleListener, BulkFileListener, BulkAwareDocumentListener.Simple, Disposable {
		@Override
		public void after(@NotNull List<? extends VFileEvent> events) {
			for(VFileEvent event : events) {
				if ((event instanceof VFileDeleteEvent || event instanceof VFileContentChangeEvent ||
						event instanceof VFileCreateEvent) && isConfigSource(event.getFile())) {
					processChangedConfigSource(event.getFile());
				}
			}
		}

		@Override
		public void afterDocumentChange(@NotNull Document document) {
			final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
			if (file != null && isConfigSource(file)) {
				processChangedConfigSource(file);
			}
		}

		@Override
		public void beforeModuleRemoved(@NotNull Project project, @NotNull Module module) {
			evict(module);
		}

		private void processChangedConfigSource(VirtualFile file) {
			Module javaProject = PsiUtilsLSImpl.getInstance(project).getModule(file);
			if (javaProject != null) {
				PsiMicroProfileProject mpProject = getJDTMicroProfileProject(javaProject);
				if (mpProject != null) {
					mpProject.evictConfigSourcesCache();
				}
			}
		}

		private void evict(Module javaProject) {
			if (javaProject != null) {
				// Remove the JDTMicroProfile project instance from the cache.
				projects.remove(javaProject);
			}
		}

		@Override
		public void dispose() {}
	}

	private PsiMicroProfileProjectManager(Project project) {
		this.project = project;
		this.projects = new HashMap<>();
		initialize();
	}

	public PsiMicroProfileProject getJDTMicroProfileProject(Module project) {
		return getJDTMicroProfileProject(project, true);
	}

	private PsiMicroProfileProject getJDTMicroProfileProject(Module project, boolean create) {
		Module javaProject = project;
		PsiMicroProfileProject info = projects.get(javaProject);
		if (info == null) {
			if (!create) {
				return null;
			}
			info = new PsiMicroProfileProject(javaProject);
			projects.put(javaProject, info);
		}
		return info;
	}

	public boolean isConfigSource(VirtualFile file) {
		String fileName = file.getName();
		for (IConfigSourceProvider provider : IConfigSourceProvider.EP_NAME.getExtensions()) {
			if (provider.isConfigSource(fileName)) {
				return true;
			}
		}
		return false;
	}

	public void initialize() {
		if (microprofileProjectListener != null) {
			return;
		}
		microprofileProjectListener = new MicroProfileProjectListener();
		MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(project);
		connection.subscribe(VirtualFileManager.VFS_CHANGES, microprofileProjectListener);
		project.getMessageBus().connect(project).subscribe(ProjectTopics.MODULES, microprofileProjectListener);
		EditorFactory.getInstance().getEventMulticaster().addDocumentListener(microprofileProjectListener, microprofileProjectListener);
	}
}
