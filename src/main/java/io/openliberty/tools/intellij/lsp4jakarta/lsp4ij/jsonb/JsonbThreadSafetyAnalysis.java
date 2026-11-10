package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb;

public class JsonbThreadSafetyAnalysis {
    /**
     * Helper class to track thread safety analysis state for a method.
     * Tracks whether a method uses Jsonb, has close() calls, thread sources,
     * and whether a Jsonb instance is created locally in the method.
     */
    boolean methodUsesJsonb = false;
    boolean hasClose = false;
    boolean hasLocalJsonbInstance = false;
    int threadSourceCount = 0;
}
