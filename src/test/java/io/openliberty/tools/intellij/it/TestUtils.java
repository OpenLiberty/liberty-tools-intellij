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

    enum TraceSevLevel {
        INFO, ERROR
    }

    /**
     * Validates that the Liberty server is no longer running.
     *
     * @param wlpInstallPath The liberty installation relative path.
     */
    public static void validateLibertyServerStopped(String testName, String wlpInstallPath) {
        printTrace(TraceSevLevel.INFO, testName + ":validateLibertyServerStopped: Entry.");

        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        int maxAttempts = 40;
        int retryIntervalSecs = 5;
        boolean foundStoppedMsg = false;

        // Find message CWWKE0036I: The server x stopped after y seconds
        for (int retryCount = 0; retryCount < maxAttempts; retryCount++) {
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
                    Thread.sleep(retryIntervalSecs * 1000);
                }
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                Assertions.fail("File: " + wlpMsgLogPath + ", could not be found.");

            } catch (Exception e) {
                e.printStackTrace();

                try {
                    Thread.sleep(retryIntervalSecs * 1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        if (!foundStoppedMsg) {
            // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
            String msgHeader = "TESTCASE: " + testName;
            printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
            String msg = testName + ":validateLibertyServerStopped: Exit. Timed out waiting for message CWWKE0036I in log:" + wlpMsgLogPath;
            printTrace(TraceSevLevel.ERROR, msg);
            Assertions.fail(msg);
        } else {
            printTrace(TraceSevLevel.INFO, testName + ":validateLibertyServerStopped: Exit. The server stopped Successfully.");
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
        printTrace(TraceSevLevel.INFO, testName + ":validateAppStarted: Entry. URL: " + appUrl);

        int retryCountLimit = 75;
        int retryIntervalSecs = 5;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status;
            try {
                URL url = new URL(appUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                status = con.getResponseCode();

                if (status != HttpURLConnection.HTTP_OK) {
                    Thread.sleep(retryIntervalSecs * 1000);
                    con.disconnect();
                    continue;
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String responseLine;
                StringBuilder content = new StringBuilder();
                while ((responseLine = br.readLine()) != null) {
                    content.append(responseLine).append(System.lineSeparator());
                }

                if (!(content.toString().contains(expectedResponse))) {
                    Thread.sleep(retryIntervalSecs * 1000);
                    con.disconnect();
                    continue;

                }
                printTrace(TraceSevLevel.INFO, testName + ":validateAppStarted. Exit. The application started successfully.");

                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(retryIntervalSecs * 1000);
                } catch (Exception ee) {
                    ee.printStackTrace(System.out);
                }
            }
        }

        // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
        String msg = testName + ":validateAppStarted: Timed out while waiting for application under URL: " + appUrl + " to become available.";
        printTrace(TraceSevLevel.ERROR, msg);
        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        String msgHeader = "Message log for failed test: " + testName + ":validateAppStarted";
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail(msg);
    }

    /**
     * Validates the application is stopped.
     *
     * @param testName       The name of the test calling this method.
     * @param appUrl         The application URL.
     * @param wlpInstallPath The liberty installation relative path.
     */
    public static void validateAppStopped(String testName, String appUrl, String wlpInstallPath) {
        printTrace(TraceSevLevel.INFO, testName + ":validateAppStopped: Entry. URL: " + appUrl);

        int retryCountLimit = 60;
        int retryIntervalSecs = 2;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status;
            try {
                URL url = new URL(appUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                status = con.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    Thread.sleep(retryIntervalSecs * 1000);
                    con.disconnect();
                    continue;
                }

                printTrace(TraceSevLevel.INFO, testName + ":validateAppStopped. Exit. The application stopped successfully.");
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(retryIntervalSecs * 1000);
                } catch (Exception ee) {
                    ee.printStackTrace(System.out);
                }
            }
        }

        // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
        String msg = testName + ":validateAppStopped: Timed out while waiting for application under URL: " + appUrl + " to stop.";
        printTrace(TraceSevLevel.ERROR, msg);
        String wlpMsgLogPath = wlpInstallPath + "/wlp/usr/servers/defaultServer/logs/messages.log";
        String msgHeader = "Message log for failed test: " + testName + ":validateAppStopped";
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail(msg);
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
        int retryIntervalSecs = 1;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;

            boolean fileExists = fileExists(pathToTestReport.toAbsolutePath());
            if (!fileExists) {
                try {
                    Thread.sleep(retryIntervalSecs * 1000);
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
     * @throws IOException if an I/O error occurs reading from the file or a malformed or unmappable byte sequence is read
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
     * Returns true if the current process is running on a Windows environment. False, otherwise.
     *
     * @return True if the current process is running on a Windows environment. False, otherwise.
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
        return f.exists();
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
            for (File value : files) {
                deleteDirectory(value);
            }
        }
        return file.delete();
    }

    /**
     * Prints a formatted message to STDOUT.
     *
     * @param traceSevLevel The severity level
     * @param msg           The message to print.
     */
    public static void printTrace(TraceSevLevel traceSevLevel, String msg) {
        switch (traceSevLevel) {
            case INFO -> System.out.println("INFO: " + java.time.LocalDateTime.now() + ": " + msg);
            case ERROR -> System.out.println("ERROR: " + java.time.LocalDateTime.now() + ": " + msg);
            default -> {
            }
        }
    }

    /**
     * Calls Thread.sleep() and ignores any exceptions.
     *
     * @param seconds The amount of seconds to sleep.
     */
    public static void sleepAndIgnoreException(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
