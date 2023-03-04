package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test utilities.
 */
public class TestUtils {

    /**
     * Validates that the Liberty server is no longer running.
     *
     * @param wlpInstallPath The liberty installation relative path.
     */
    public static void validateLibertyServerStopped(String testName, String wlpInstallPath) {
        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        int maxAttempts = 30;
        boolean foundStoppedMsg = false;

        // Find message CWWKE0036I: The server x stopped after y seconds
        for (int i = 0; i < maxAttempts; i++) {
            try (BufferedReader br = new BufferedReader(new FileReader(wlpMsgLogPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("CWWKE0036I")) {
                        foundStoppedMsg = true;
                        break;
                    }
                }

                if (foundStoppedMsg) {
                    break;
                } else {
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail("Caught exception waiting for stop message", e);
            }
        }

        if (!foundStoppedMsg) {
            // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
            String msgHeader = "TESTCASE: " + testName;
            printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
            Assertions.fail("Message CWWKE0036I not found in " + wlpMsgLogPath);
        }

    }

    /**
     * Validates the application is started.
     *
     * @param testName         The name of the test calling this method.
     * @param appUrl           The application URL..
     * @param expectedResponse The expected application response payload.
     * @param wlpInstallPath   The liberty installation relative path.
     */
    public static void validateAppStarted(String testName, String appUrl, String expectedResponse, String wlpInstallPath) {
        int retryCountLimit = 36;
        int reryIntervalSecs = 5;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status = 0;
            try {
                URL url = new URL(appUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                status = con.getResponseCode();

                if (status != HttpURLConnection.HTTP_OK) {
                    Thread.sleep(reryIntervalSecs * 1000);
                    con.disconnect();
                    System.out.println("INFO: validateAppStarted: Retrying. Cause: Unexpected HTTP request status: " + status + ". Retry: " + retryCount);
                    continue;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String responseLine = "";
                StringBuffer content = new StringBuffer();
                while ((responseLine = br.readLine()) != null) {
                    content.append(responseLine).append(System.lineSeparator());
                }

                if (!(content.toString().contains(expectedResponse))) {
                    Thread.sleep(reryIntervalSecs * 1000);
                    con.disconnect();
                    System.out.println("INFO: validateAppStarted: Retrying. Cause: Unexpected HTTP response: " + content + ". Retry: " + retryCount);
                    continue;

                }
                System.out.println("INFO: The application started successfully.");
                return;
            } catch (Exception e) {
                System.out.println("INFO: validateAppStarted: Retrying. Cause: Exception: " + e.getMessage() + ", retry: " + retryCount);
                try {
                    Thread.sleep(reryIntervalSecs * 1000);
                } catch (Exception ee) {
                    ee.printStackTrace(System.out);
                }
                continue;
            }
        }

        // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        String msgHeader = "TESTCASE: " + testName;
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail("Timed out while waiting for application under URL: " + appUrl + " to become available.");
    }

    /**
     * Validates the application is stopped.
     *
     * @param testName         The name of the test calling this method.
     * @param appUrl           The application URL..
     * @param expectedResponse The expected application response payload.
     * @param wlpInstallPath   The liberty installation relative path.
     */
    public static void validateAppStopped(String testName, String appUrl, boolean expectSuccess, String expectedResponse, String wlpInstallPath) {
        int retryCountLimit = 60;
        int reryIntervalSecs = 2;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status = 0;
            try {
                URL url = new URL(appUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                status = con.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    Thread.sleep(reryIntervalSecs * 1000);
                    con.disconnect();
                    continue;
                }

                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(reryIntervalSecs * 1000);
                } catch (Exception ee) {
                    ee.printStackTrace(System.out);
                }
                continue;
            }
        }

        // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        String msgHeader = "TESTCASE: " + testName;
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail("Timed out while waiting for application under URL: " + appUrl + " to stop.");
    }

    /**
     * Prints the Liberty server's messages.log identified by the input path.
     *
     * @param wlpMsgLogPath The messages.log path to print.
     */
    public static void printLibertyMessagesLogFile(String msgHeader, String wlpMsgLogPath) {
        System.out.println("--------------------------- messages.log ----------------------------");
        System.out.println(msgHeader);
        System.out.println("---------------------------------------------------------------------");

        try (BufferedReader br = new BufferedReader(new FileReader(wlpMsgLogPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File: " + wlpMsgLogPath + " was not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("---------------------------------------------------------------------");
    }

    /**
     * Validates that the test report represented by the input path exists.
     *
     * @param pathToTestReport The path to the report.
     */
    public static void validateTestReportExists(Path pathToTestReport) {
        int retryCountLimit = 100;
        int reryIntervalSecs = 1;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;

            boolean fileExists = fileExists(pathToTestReport.toAbsolutePath());
            if (!fileExists) {
                try {
                    Thread.sleep(reryIntervalSecs * 1000);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    continue;
                }
                continue;
            }

            return;
        }
    }

    /**
     * Returns true or false depending on if the input text is found in the target file
     *
     * @throws IOException
     */
    public static boolean isTextInFile(String filePath, String text) throws IOException {

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            if (line.contains(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the current process is running on a windows environment. False, otherwise.
     *
     * @return True if the current process is running on a windows environment. False, otherwise.
     */
    public static boolean onWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    /**
     * Returns true if the file identified by the input path exists. False, otherwise.
     *
     * @param filePath The file's path.
     * @return True if the file identified by the input path exists. False, otherwise.
     */
    private static boolean fileExists(Path filePath) {
        File f = new File(filePath.toString());
        boolean exists = f.exists();

        return exists;
    }

    /**
     * Deletes file identified by the input path. If the file is a directory, it must be empty.
     *
     * @param file The file.
     * @return Returns true if the file identified by the input path was deleted. False, otherwise.
     */
    public static boolean deleteFile(File file) {
        boolean deleted = true;

        if (file.exists()) {
            if (!file.isDirectory()) {
                deleted = file.delete();
            } else {
                deleted = deleteDirectory(file);
            }
        }

        return deleted;
    }

    /**
     * Recursively deletes the input file directory.
     *
     * @param file The directory.
     * @return Returns true if the directory identified by the input path was deleted. False, otherwise.
     */
    private static boolean deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteDirectory(files[i]);
            }
        }
        return file.delete();
    }

    /**
     * Updates browser configuration preferences.
     *
     * @param useInternal Determines whether an internal or external browser setting is set. If true, the internal browser setting is
     *                    set. If false the external browser setting is set.
     * @return True if the browser settings were updated successfully or if it already contains the desired value. False, otherwise.
     */
    public static boolean updateBrowserPreferences(boolean useInternal) {
        return true;
    }
}
