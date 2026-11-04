package io.openliberty.tools.intellij.lsp4jakarta.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * Detects Jakarta EE version by checking for version-specific class signatures.
 * This strategy analyzes the actual API capabilities available in the classpath
 * by checking for the presence of version-specific classes and packages across
 * all Jakarta EE modules.
 *
 * This approach is independent of JAR naming, manifest metadata, or build tools,
 * making it the most reliable detection method for any project type.
 */
public class ClassSignatureVersionDetector {

    /**
     * Analyzes Jakarta version based on class signatures.
     * Checks for version-specific classes across all Jakarta modules.
     *
     * @param project The IntelliJ project for class resolution
     * @return The detected Jakarta version based on available classes
     */
    public List<JakartaVersion> detectVersion(Project project) {
        if (project == null || project.isDisposed()) {
            return new ArrayList<>();
        }

        Map<Integer, JakartaVersion> availableVersions = new HashMap<>();
        Map<JakartaVersion, Integer> versionScores = new HashMap<>();
        versionScores.put(JakartaVersion.EE_11, 0);
        versionScores.put(JakartaVersion.EE_10, 0);
        versionScores.put(JakartaVersion.EE_9, 0);

        Set<String> detectedFeatures = new HashSet<>();

        System.out.println("Class Signature Analysis (All Modules)-----------");

        // Check all modules for each version
        checkServletVersion(project, versionScores, detectedFeatures, availableVersions);
        checkPersistenceVersion(project, versionScores, detectedFeatures, availableVersions);
        checkCDIVersion(project, versionScores, detectedFeatures, availableVersions);
        checkFacesVersion(project, versionScores, detectedFeatures, availableVersions);
        checkRESTVersion(project, versionScores, detectedFeatures, availableVersions);
        checkWebSocketVersion(project, versionScores, detectedFeatures, availableVersions);
        checkJSONVersion(project, versionScores, detectedFeatures, availableVersions);
        checkJSONBVersion(project, versionScores, detectedFeatures, availableVersions);
        checkAnnotationVersion(project, versionScores, detectedFeatures, availableVersions);
        checkEJBVersion(project, versionScores, detectedFeatures, availableVersions);
        checkTransactionVersion(project, versionScores, detectedFeatures, availableVersions);
        checkValidationVersion(project, versionScores, detectedFeatures, availableVersions);
        checkInterceptorVersion(project, versionScores, detectedFeatures, availableVersions);
        checkInjectVersion(project, versionScores, detectedFeatures, availableVersions);
        checkSecurityVersion(project, versionScores, detectedFeatures, availableVersions);
        checkDataVersion(project, versionScores, detectedFeatures, availableVersions);

        // Determine version based on highest score
        JakartaVersion detectedVersion = JakartaVersion.UNKNOWN;
        int maxScore = 0;
        for (Map.Entry<JakartaVersion, Integer> entry : versionScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                detectedVersion = entry.getKey();
            }
        }

        System.out.println("Detected Features:");
        detectedFeatures.forEach(feature -> System.out.println("  - " + feature));

        System.out.println("------------------------------------------------------------------");
        System.out.println("Class Signature Detected Version: ");
        availableVersions.forEach((key, version) -> {
            System.out.println(key + ":" + version);
        });
        System.out.println("------------------------------------------------------------------");

        return new ArrayList<>(availableVersions.values());
    }

    private void checkServletVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.servlet.ServletConnection")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Servlet 6.1 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.servlet.ServletContext") &&
                   methodExists(project, "jakarta.servlet.ServletContext", "getRequestCharacterEncoding")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Servlet 6.0 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.servlet.Servlet")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("Servlet 5.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkPersistenceVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.persistence.criteria.CriteriaSelect")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Persistence 3.2 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.persistence.EntityManagerFactory") &&
                   methodExists(project, "jakarta.persistence.EntityManagerFactory", "runInTransaction")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Persistence 3.1 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.persistence.Entity")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("Persistence 3.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkCDIVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("CDI 4.1 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.enterprise.inject.build.compatible.spi.BeanInfo")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("CDI 4.0 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.enterprise.context.ApplicationScoped")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("CDI 3.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkFacesVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.faces.annotation.View")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Faces 4.1 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.faces.push.Push")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Faces 4.0 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.faces.component.UIComponent")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("Faces 3.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkRESTVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.ws.rs.SeBootstrap") ||
            classExists(project, "jakarta.ws.rs.core.EntityPart")) {
            // REST 3.1/4.0 share the same API - both in EE 10 and EE 11
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            features.add("REST 3.1/4.0 (EE 10/11)");
        } else if (classExists(project, "jakarta.ws.rs.Path")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("REST 3.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkWebSocketVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.websocket.Session") &&
            methodExists(project, "jakarta.websocket.Session", "getRequestParameterMap")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("WebSocket 2.2 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.websocket.ClientEndpointConfig")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("WebSocket 2.1 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.websocket.Endpoint")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("WebSocket 2.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkJSONVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.json.JsonPatch")) {
            // JSON-P 2.1 is in both EE 10 and EE 11
            // We'll score both, but other modules will help differentiate
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            features.add("JSON-P 2.1 (EE 10/11)");
        } else if (classExists(project, "jakarta.json.JsonValue")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("JSON-P 2.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkJSONBVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.json.bind.Jsonb")) {
            // JSON-B 3.0 is in both EE 10 and EE 11
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            features.add("JSON-B 3.0 (EE 10/11)");
        } else if (classExists(project, "jakarta.json.bind.JsonbBuilder")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("JSON-B 2.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkAnnotationVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.annotation.ManagedBean")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Annotation 3.0 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.annotation.sql.DataSourceDefinitions")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Annotation 2.1 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.annotation.PostConstruct")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("Annotation 2.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkEJBVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.ejb.Stateless")) {
            // EJB 4.0 is in EE 9, EE 10, and EE 11
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
            features.add("EJB 4.0 (EE 9/10/11)");
        }
    }

    private void checkTransactionVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.transaction.Transactional")) {
            // Transaction 2.0 is in EE 9, EE 10, and EE 11
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
            features.add("Transaction 2.0 (EE 9/10/11)");
        }
    }

    private void checkValidationVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.validation.valueextraction.ValueExtractor")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Validation 3.1 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.validation.constraints.NotNull")) {
            // Validation 3.0 is in both EE 9 and EE 10
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
            features.add("Validation 3.0 (EE 9/10)");
        }
    }

    private void checkInterceptorVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.interceptor.InvocationContext") &&
            methodExists(project, "jakarta.interceptor.InvocationContext", "getInterceptorBindings")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Interceptor 2.2 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.interceptor.InvocationContext") &&
                   methodExists(project, "jakarta.interceptor.InvocationContext", "getContextData")) {
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Interceptor 2.1 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.interceptor.Interceptor")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
            features.add("Interceptor 2.0 (EE 9)");
        }
    }

    private void checkInjectVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.inject.Inject")) {
            // Inject 2.0 is in EE 9, EE 10, and EE 11
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
            features.add("Inject 2.0 (EE 9/10/11)");
        }
    }

    private void checkSecurityVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Security 4.0 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        } else if (classExists(project, "jakarta.security.enterprise.identitystore.IdentityStore")) {
            // Security 3.0 is in EE 10
            scores.put(JakartaVersion.EE_10, scores.get(JakartaVersion.EE_10) + 1);
            features.add("Security 3.0 (EE 10)");
            availableVersions.put(JakartaVersion.EE_10.getLevel(), JakartaVersion.EE_10);
        } else if (classExists(project, "jakarta.security.enterprise.SecurityContext")) {
            scores.put(JakartaVersion.EE_9, scores.get(JakartaVersion.EE_9) + 1);
            features.add("Security 2.0 (EE 9)");
            availableVersions.put(JakartaVersion.EE_9.getLevel(), JakartaVersion.EE_9);
        }
    }

    private void checkDataVersion(Project project, Map<JakartaVersion, Integer> scores, Set<String> features, Map<Integer, JakartaVersion> availableVersions) {
        if (classExists(project, "jakarta.data.repository.Repository")) {
            scores.put(JakartaVersion.EE_11, scores.get(JakartaVersion.EE_11) + 1);
            features.add("Data 1.0 (EE 11)");
            availableVersions.put(JakartaVersion.EE_11.getLevel(), JakartaVersion.EE_11);
        }
    }

    /**
     * Checks if a class exists in the project's classpath.
     *
     * @param project The IntelliJ project
     * @param fullyQualifiedClassName The fully qualified class name
     * @return true if the class exists and is valid
     */
    private boolean classExists(Project project, String fullyQualifiedClassName) {
        if (project == null || project.isDisposed()) {
            return false;
        }
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedClassName, GlobalSearchScope.allScope(project));
        return psiClass != null && psiClass.isValid();
    }

    /**
     * Checks if a method exists in a class.
     *
     * @param project The IntelliJ project
     * @param fullyQualifiedClassName The fully qualified class name
     * @param methodName The method name to check
     * @return true if any method with this name exists in the class or its super classes/interfaces
     */
    private boolean methodExists(Project project, String fullyQualifiedClassName, String methodName) {
        if (project == null || project.isDisposed()) {
            return false;
        }
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedClassName, GlobalSearchScope.allScope(project));
        if (psiClass != null && psiClass.isValid()) {
            return psiClass.findMethodsByName(methodName, true).length > 0;
        }
        return false;
    }
}


