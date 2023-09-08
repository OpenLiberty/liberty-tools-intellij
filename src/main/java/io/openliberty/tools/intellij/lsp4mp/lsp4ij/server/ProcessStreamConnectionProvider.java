package io.openliberty.tools.intellij.lsp4mp.lsp4ij.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ProcessStreamConnectionProvider implements StreamConnectionProvider{
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStreamConnectionProvider.class);

    private @Nullable Process process;
    private List<String> commands;
    private @Nullable String workingDir;

    public ProcessStreamConnectionProvider(String serverType) {
        String javaHome = System.getProperty("java.home");
        checkJavaHome(javaHome, serverType);
    }

    public ProcessStreamConnectionProvider(List<String> commands) {
        this.commands = commands;
    }

    public ProcessStreamConnectionProvider(List<String> commands, String workingDir) {
        this.commands = commands;
        this.workingDir = workingDir;
    }

    @Override
    public void start() throws IOException {
        if (this.commands == null || this.commands.isEmpty() || this.commands.stream().anyMatch(Objects::isNull)) {
            throw new IOException("Unable to start language server: " + this.toString()); //$NON-NLS-1$
        }

        ProcessBuilder builder = createProcessBuilder();
        Process p = builder.start();
        this.process = p;
        if (!p.isAlive()) {
            throw new IOException("Unable to start language server: " + this.toString()); //$NON-NLS-1$
        } else {
            LOGGER.info("Starting language server: " + this.toString());
        }
    }

    protected ProcessBuilder createProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder(getCommands());
        if (getWorkingDirectory() != null) {
            builder.directory(new File(getWorkingDirectory()));
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return builder;
    }

    @Override
    public @Nullable InputStream getInputStream() {
        Process p = process;
        return p == null ? null : p.getInputStream();
    }

    @Override
    public @Nullable InputStream getErrorStream() {
        Process p = process;
        return p == null ? null : p.getErrorStream();
    }

    @Override
    public @Nullable OutputStream getOutputStream() {
        Process p = process;
        return p == null ? null : p.getOutputStream();
    }

    @Override
    public void stop() {
        Process p = process;
        if (p != null) {
            p.destroy();
        }
    }

    protected List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    protected @Nullable String getWorkingDirectory() {
        return workingDir;
    }

    public void setWorkingDirectory(String workingDir) {
        this.workingDir = workingDir;
    }

    protected boolean checkJavaVersion(String javaHome, int expectedVersion) {
        final ProcessBuilder builder = new ProcessBuilder(javaHome +
                File.separator + "bin" + File.separator + "java", "-version");
        try {
            final Process p = builder.start();
            final Reader r = new InputStreamReader(p.getErrorStream());
            final StringBuilder sb = new StringBuilder();
            int i;
            while ((i = r.read()) != -1) {
                sb.append((char) i);
            }
            return parseMajorJavaVersion(sb.toString()) >= expectedVersion;
        }
        catch (IOException ioe) {}
        return false;
    }

    private int parseMajorJavaVersion(String content) {
        final String versionRegex = "version \"(.*)\"";
        Pattern p = Pattern.compile(versionRegex);
        Matcher m = p.matcher(content);
        if (!m.find()) {
            return 0;
        }
        String version = m.group(1);

        // Ignore '1.' prefix for legacy Java versions
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }

        // Extract the major version number.
        final String numberRegex = "\\d+";
        p = Pattern.compile(numberRegex);
        m = p.matcher(version);
        if (!m.find()) {
            return 0;
        }
        return Integer.parseInt(m.group());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ProcessStreamConnectionProvider)) {
            return false;
        }
        ProcessStreamConnectionProvider other = (ProcessStreamConnectionProvider) obj;
        return Objects.equals(this.getCommands(), other.getCommands())
                && Objects.equals(this.getWorkingDirectory(), other.getWorkingDirectory());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getCommands(), this.getWorkingDirectory());
    }

    @Override
    public String toString() {
        return "ProcessStreamConnectionProvider [commands=" + this.getCommands() + ", workingDir=" //$NON-NLS-1$//$NON-NLS-2$
                + this.getWorkingDirectory() + "]"; //$NON-NLS-1$
    }
    public void checkJavaHome(String javaHome, String serverType) {

        if (javaHome == null) {
            String errorMessage = LocalizedResourceUtil.getMessage("liberty.action.selection.dialog.java-home.null");
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
            return;
        }

        File javaHomeDir = new File(javaHome);
        if (!javaHomeDir.exists()) {
            String errorMessage = LocalizedResourceUtil.getMessage("liberty.action.selection.dialog.javaHomeDir.exist") + javaHome;
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
            return;
        }

        if (!checkJavaVersion(javaHome, Constants.REQUIRED_JAVA_VERSION)) {
            String errorMessage =LocalizedResourceUtil.getMessage( "liberty.action.selection.dialog.java-version",Constants.REQUIRED_JAVA_VERSION,serverType);
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
        }
    }

    private void showErrorPopup(String errorMessage) {
        // Wrap the errorMessage in HTML tags to create a two-line popup
        String htmlErrorMessage = "<html><body style='width: 200px; text-align: center;'>" + errorMessage + "</body></html>";

        JOptionPane.showMessageDialog(null, htmlErrorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

}
