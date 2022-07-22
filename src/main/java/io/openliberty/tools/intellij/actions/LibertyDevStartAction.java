package io.openliberty.tools.intellij.actions;

import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;



public class LibertyDevStartAction extends LibertyGeneralAction {

    public LibertyDevStartAction() {
        setActionCmd("start Liberty dev mode");
    }

    @Override
    protected void executeLibertyAction() {
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        String startCmd = "https://start.openliberty.io/api/start?a=app-name&b=gradle&e=9.1&g=com.demo&j=11&m=5.0";

        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(startCmd))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            InputStream in = new BufferedInputStream(response.body());

            String home = System.getProperty("user.home");
            File file = new File(new File(home, "Downloads"), "app-name.zip");


            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                int read;
                byte[] bytes = new byte[8192];
                while ((read = in.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }
        } catch (URISyntaxException e) {
            System.out.println("uhoh");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("uhoh2");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("uhoh3");
            e.printStackTrace();
        }

       /*if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "curl -o ./app-name.zip https://start.openliberty.io/api/start?a=app-name&b=maven&e=9.1&g=com.demo&j=11&m=5.0";
            //startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev -f \"" + buildFile.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            //startCmd = "gradle libertyDev -b=" + buildFile.getCanonicalPath();
            startCmd = "curl -o ./app-name.zip https://start.openliberty.io/api/start?a=app-name&b=maven&e=9.1&g=com.demo&j=11&m=5.0";
        }*/
        if (widget == null) {
            log.debug("Unable to start Liberty dev mode, could not get or create terminal widget for " + projectName);
            //return;
        }
        //LibertyActionUtil.executeCommand(widget, startCmd);
    }
}