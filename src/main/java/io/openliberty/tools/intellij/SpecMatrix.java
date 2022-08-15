package io.openliberty.tools.intellij;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
public class SpecMatrix {
    private Map<String, MicroProfileConfig> configs = new TreeMap<>();
    private Map<String, String> descriptions = new TreeMap<>();

    public Map<String, MicroProfileConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, MicroProfileConfig> configs) {
        this.configs = configs;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }

}