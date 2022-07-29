package io.openliberty.tools.intellij;


import java.util.ArrayList;
import java.util.List;

/**
 * @author Ehsan Zaery Moghaddam (zaerymoghaddam@gmail.com)
 */
public class MicroProfileConfig {
    private List<String> supportedServers = new ArrayList<>();
    private List<String> specs = new ArrayList<>();
    private List<String> specCodes = new ArrayList<>();

    public List<String> getSpecCodes() {
        return specCodes;
    }

    public void setSpecCodes(List<String> specCodes) {
        this.specCodes = specCodes;
    }

    public List<String> getSupportedServers() {
        return supportedServers;
    }

    public void setSupportedServers(List<String> supportedServers) {
        this.supportedServers = supportedServers;
    }

    public List<String> getSpecs() {
        return specs;
    }

    public void setSpecs(List<String> specs) {
        this.specs = specs;
    }
}