package io.openliberty.tools.intellij.lsp4jakarta.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Collections;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;

/**
 * Main facade for Jakarta EE version detection.
 * Uses multiple detection strategies to identify the Jakarta EE version.
 */
public class JakartaVersionFinder {

    /**
     * Detection strategy enum to specify which detection method to use.
     */
    public enum DetectionStrategy {
        /** Use JAR filename parsing (default, backward compatible) */
        FILENAME,
        /** Use JAR manifest inspection */
        MANIFEST,
        /** Use class signature detection */
        CLASS_SIGNATURE,
        /** Try manifest first, fallback to filename */
        MANIFEST_THEN_FILENAME,
        /** Try filename first, fallback to manifest */
        FILENAME_THEN_MANIFEST,
        /** Try class signature first, fallback to manifest, then filename */
        CLASS_SIGNATURE_THEN_MANIFEST_THEN_FILENAME
    }

    private static final JarFilenameVersionDetector filenameDetector = new JarFilenameVersionDetector();
    private static final JarManifestVersionDetector manifestDetector = new JarManifestVersionDetector();
    private static final ClassSignatureVersionDetector classSignatureDetector = new ClassSignatureVersionDetector();

    /**
     * Analyzes classpath using the default detection strategy (manifest then filename).
     *
     * @param entries The classpath entries to analyze
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(VirtualFile[] entries) {
        return analyzeClasspath(entries,  null, DetectionStrategy.MANIFEST_THEN_FILENAME);
    }

    /**
     * Analyzes classpath with a Project context using the default detection strategy.
     *
     * @param entries The classpath entries to analyze
     * @param project The IntelliJ project
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(VirtualFile[] entries, Project project) {
        return analyzeClasspath(entries, project, DetectionStrategy.MANIFEST_THEN_FILENAME);
    }

    /**
     * Analyzes classpath using the specified detection strategy.
     *
     * @param entries The classpath entries to analyze
     * @param strategy The detection strategy to use
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(VirtualFile[] entries, DetectionStrategy strategy) {
        return analyzeClasspath(entries,  null, strategy);
    }

    /**
     * Analyzes classpath using the specified detection strategy with Project context.
     *
     * @param entries The classpath entries to analyze
     * @param project The IntelliJ project (required for CLASS_SIGNATURE strategy)
     * @param strategy The detection strategy to use
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(VirtualFile[] entries, Project project, DetectionStrategy strategy) {
        List<JakartaVersion> detectedVersions = new ArrayList<>();
        VirtualFile[] validEntries = entries != null ? entries : new VirtualFile[0];

        switch (strategy) {
            case MANIFEST:
                detectedVersions = manifestDetector.detectVersion(validEntries);
                break;

            case FILENAME:
                detectedVersions = filenameDetector.detectVersion(validEntries);
                break;

            case CLASS_SIGNATURE:
                if (project != null) {
                    detectedVersions = classSignatureDetector.detectVersion(project);
                } else {
                    System.out.println("CLASS_SIGNATURE strategy requires Project, falling back to MANIFEST");
                    detectedVersions = manifestDetector.detectVersion(validEntries);
                }
                break;

            case MANIFEST_THEN_FILENAME:
                detectedVersions = manifestDetector.detectVersion(validEntries);
                if (isUnknownVersion(detectedVersions)) {
                    System.out.println("Manifest detection inconclusive, trying filename detection...");
                    detectedVersions = filenameDetector.detectVersion(validEntries);
                }
                break;

            case FILENAME_THEN_MANIFEST:
                detectedVersions = filenameDetector.detectVersion(validEntries);
                if (isUnknownVersion(detectedVersions)) {
                    System.out.println("Filename detection inconclusive, trying manifest detection...");
                    detectedVersions = manifestDetector.detectVersion(validEntries);
                }
                break;

            case CLASS_SIGNATURE_THEN_MANIFEST_THEN_FILENAME:
                if (project != null) {
                    detectedVersions = classSignatureDetector.detectVersion(project);
                }
                if (isUnknownVersion(detectedVersions)) {
                    System.out.println("Class signature detection inconclusive, trying manifest detection...");
                    detectedVersions = manifestDetector.detectVersion(validEntries);
                }
                if (isUnknownVersion(detectedVersions)) {
                    System.out.println("Manifest detection inconclusive, trying filename detection...");
                    detectedVersions = filenameDetector.detectVersion(validEntries);
                }
                break;

            default:
                detectedVersions = filenameDetector.detectVersion(validEntries);
                break;
        }

        // Fallback to JEE9 if version is still unknown
        if (isUnknownVersion(detectedVersions)) {
            System.out.println("UNKNOWN version: fall back to JEE9");
            detectedVersions.add(JakartaVersion.EE_9);
        }

        return detectedVersions;
    }

    /**
     * Analyzes classpath for a given PsiJavaFile using the default detection strategy.
     *
     * @param file The PSI Java file
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(PsiJavaFile file) {
        return analyzeClasspath(file, DetectionStrategy.MANIFEST_THEN_FILENAME);
    }

    /**
     * Analyzes classpath for a given PsiJavaFile using the specified detection strategy.
     *
     * @param file The PSI Java file
     * @param strategy The detection strategy to use
     * @return The detected Jakarta version
     */
    public static List<JakartaVersion> analyzeClasspath(PsiJavaFile file, DetectionStrategy strategy) {
        if (file == null || !file.isValid()) {
            return Collections.singletonList(JakartaVersion.EE_9);
        }
        VirtualFile[] entries = getResolvedClasspathEntries(file);
        return analyzeClasspath(entries, file.getProject(), strategy);
    }

    /**
     * Resolves classpath entries (classes roots) for a given PsiJavaFile.
     *
     * @param file The PSI Java file
     * @return array of VirtualFile classpath roots
     */
    public static VirtualFile[] getResolvedClasspathEntries(PsiJavaFile file) {
        if (file == null || !file.isValid()) {
            return new VirtualFile[0];
        }
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        if (module != null) {
            return ModuleRootManager.getInstance(module)
                    .orderEntries()
                    .recursively()
                    .classes()
                    .getRoots();
        }
        return new VirtualFile[0];
    }

    /**
     * Returns a static list of all known Jakarta EE versions (highest first).
     *
     * @return all known Jakarta EE versions as a list
     */
    public static List<JakartaVersion> getAllKnownVersions() {
        return Arrays.asList(JakartaVersion.EE_11, JakartaVersion.EE_10, JakartaVersion.EE_9, JakartaVersion.EE_8);
    }

    private static boolean isUnknownVersion(List<JakartaVersion> detectedVersions) {
        return detectedVersions == null
               || detectedVersions.isEmpty()
               || detectedVersions.stream().allMatch(v -> v == JakartaVersion.UNKNOWN);
    }


}
