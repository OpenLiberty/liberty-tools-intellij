/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
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
     * WLP messages.log path.
     */
    public static final Path WLP_MSGLOG_PATH = Paths.get("wlp", "usr", "servers", "defaultServer", "logs", "messages.log");

    /**
     * Liberty server stopped message:
     * CWWKE0036I: The server defaultServer stopped after 12.25 seconds.
     */
    public static final String SEVER_STOPPED_MSG = "CWWKE0036I";

    enum TraceSevLevel {
        INFO, ERROR
    }

    /**
     * Validates that the Liberty server is no longer running.
     *
     * @param testName       The name of the test calling this method.
     * @param wlpInstallPath The liberty installation relative path.
     */
    public static void validateLibertyServerStopped(String testName, String wlpInstallPath) {
        validateLibertyServerStopped(testName, wlpInstallPath, 40, true);
    }

    /**
     * Validates that the Liberty server is no longer running.
     *
     * @param testName       The name of the test calling this method.
     * @param wlpInstallPath The liberty installation relative path.
     * @param maxAttempts    The number of retries to validate the server has stopped.
     * @param failOnNoStop   The indicator to fail if the server did not stop.
     */
    public static void validateLibertyServerStopped(String testName, String wlpInstallPath, int maxAttempts, boolean failOnNoStop) {
        printTrace(TraceSevLevel.INFO, testName + ":validateLibertyServerStopped: Entry.");

        String wlpMsgLogPath = Paths.get(wlpInstallPath, WLP_MSGLOG_PATH.toString()).toString();
        int retryIntervalSecs = 5;
        boolean foundStoppedMsg = false;
        Exception error = null;

        // Find the server stopped message.
        for (int retryCount = 0; retryCount < maxAttempts; retryCount++) {
            error = null;
            try (BufferedReader br = new BufferedReader(new FileReader(wlpMsgLogPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(SEVER_STOPPED_MSG)) {
                        foundStoppedMsg = true;
                        break;
                    }
                }

                if (foundStoppedMsg) {
                    break;
                } else {
                    sleepAndIgnoreException(retryIntervalSecs);
                }
            } catch (FileNotFoundException fnfe) {
                Assertions.fail("File: " + wlpMsgLogPath + ", could not be found.", fnfe);
            } catch (Exception e) {
                error = e;
                sleepAndIgnoreException(retryIntervalSecs);
            }
        }

        if (!foundStoppedMsg) {
            // If instructed not to fail the test on error, report the last error back to the caller.
            if (!failOnNoStop) {
                String cause = (error != null) ? "Cause: " + error.getMessage() : "Cause: " + SEVER_STOPPED_MSG + " not found in server log.";
                throw new RuntimeException("Unable to verify that the Liberty server stopped. " + cause);
            }

            // Print the Liberty server's messages.log and fail.
            String msgHeader = "TESTCASE: " + testName;
            printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
            String msg = testName + ":validateLibertyServerStopped: Exit. Timed out waiting for message " + SEVER_STOPPED_MSG + " in log:" + wlpMsgLogPath;
            printTrace(TraceSevLevel.ERROR, msg);
            if (error == null) {
                Assertions.fail(msg);
            } else {
                Assertions.fail(msg, error);
            }
        } else {
            printTrace(TraceSevLevel.INFO, testName + ":validateLibertyServerStopped: Exit. The server stopped successfully.");
        }
    }

    /**
     * Validates that the project is started.
     *
     * @param testName         The name of the test calling this method.
     * @param resourceURI      The project resource URI.
     * @param port             The port number to reach the project
     * @param expectedResponse The expected resource response payload.
     * @param wlpInstallPath   The liberty installation relative path.
     */
    public static void validateProjectStarted(String testName, String resourceURI, int port, String expectedResponse, String wlpInstallPath, boolean findConn) {
        printTrace(TraceSevLevel.INFO, testName + ":validateProjectStarted: Entry. Port: " + port + ", resourceURI: " + resourceURI);

        int retryCountLimit = 75;
        int retryIntervalSecs = 5;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;

            HttpURLConnection conn;
            if (findConn) {
                conn = findHttpConnection(port, resourceURI);
            } else {
                conn = getHttpConnection(port, resourceURI);
            }

            if (conn == null) {
                TestUtils.sleepAndIgnoreException(retryIntervalSecs);
                continue;
            }

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String responseLine;
                StringBuilder content = new StringBuilder();
                while ((responseLine = br.readLine()) != null) {
                    content.append(responseLine).append(System.lineSeparator());
                }

                if (!(content.toString().contains(expectedResponse))) {
                    Thread.sleep(retryIntervalSecs * 1000);
                    conn.disconnect();
                    continue;

                }
                printTrace(TraceSevLevel.INFO, testName + ":validateProjectStarted. Exit. The project started successfully.");

                return;
            } catch (Exception e) {
                TestUtils.sleepAndIgnoreException(retryIntervalSecs);
            }
        }

        // If we are here, the expected outcome was not found. Print the Liberty server's messages.log and fail.
        String msg = testName + ":validateProjectStarted: Timed out while waiting for project with resource URI " + resourceURI + "and port " + port + " to become available.";
        printTrace(TraceSevLevel.ERROR, msg);
        String wlpMsgLogPath = Paths.get(wlpInstallPath, WLP_MSGLOG_PATH.toString()).toString();
        String msgHeader = "Message log for failed test: " + testName + ":validateProjectStarted";
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail(msg);
    }

    /**
     * Finds an active connection object.
     * This is done based on how the LMP/LGP finds a usable port when starting dev mode in a container.
     * If the specified port is not usable, the LMP/LGP increases the specified port number
     * by one until it finds a usable port, which, in turn, it is used to start dev mode in a container.
     * A port may not be usable if the socket associated with the specified port
     * can not yet be bound to between tests.
     *
     * @param port        The initial port number.
     * @param resourceURI The resource URI.
     * @return An active connection object..
     */
    public static HttpURLConnection findHttpConnection(int port, String resourceURI) {
        int testPort = port;
        int maxPortIncrement = 4;
        HttpURLConnection connection = null;
        for (int i = 0; i < maxPortIncrement; i++) {
            connection = getHttpConnection(testPort, resourceURI);
            if (connection != null) {
                break;
            }
            testPort += 1;
        }

        return connection;
    }

    /**
     * Returns an active connection object.
     *
     * @param port        The initial port number.
     * @param resourceURI The resource URI.
     * @return An active connection object.
     */
    public static HttpURLConnection getHttpConnection(int port, String resourceURI) {
        String resourceURL = "http://localhost:" + port + "/" + resourceURI;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(resourceURL);
            HttpURLConnection tmpConn = (HttpURLConnection) url.openConnection();
            tmpConn.setRequestMethod("GET");
            tmpConn.connect();
            int status = tmpConn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                conn = tmpConn;
            } else {
                tmpConn.disconnect();
            }
        } catch (Exception e) {
            // Ignore.
        }

        return conn;
    }

    /**
     * Validates the project stopped.
     *
     * @param testName       The name of the test calling this method.
     * @param projUrl        The project's URL.
     * @param wlpInstallPath The liberty installation relative path.
     */
    public static void validateProjectStopped(String testName, String projUrl, String wlpInstallPath) {
        printTrace(TraceSevLevel.INFO, testName + ":validateProjectStopped: Entry. URL: " + projUrl);

        int retryCountLimit = 60;
        int retryIntervalSecs = 2;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status;
            try {
                URL url = new URL(projUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                status = con.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    Thread.sleep(retryIntervalSecs * 1000);
                    con.disconnect();
                    continue;
                }

                printTrace(TraceSevLevel.INFO, testName + ":validateProjectStopped. Exit. The project stopped successfully.");
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
        String msg = testName + ":validateProjectStopped: Timed out while waiting for project under URL: " + projUrl + " to stop.";
        printTrace(TraceSevLevel.ERROR, msg);
        String wlpMsgLogPath = Paths.get(wlpInstallPath, WLP_MSGLOG_PATH.toString()).toString();
        String msgHeader = "Message log for failed test: " + testName + ":validateProjectStopped";
        printLibertyMessagesLogFile(msgHeader, wlpMsgLogPath);
        Assertions.fail(msg);
    }

    /**
     * Validates the expected hover string message was raised in popup.
     *
     * @param expectedHoverText The full string of popup data that is expected to be found.
     * @param hoverPopupText    The string found in the popup window
     */
    public static void validateHoverData(String expectedHoverText, String hoverPopupText) {

        if (hoverPopupText.contains(expectedHoverText)) {
            Assertions.assertTrue(hoverPopupText.contains(expectedHoverText));
        } else {
            Assertions.fail("Did not find diagnostic help text expected. Looking for " + expectedHoverText);
        }
    }

    /**
     * Validates the expected server.xml stanza entry is found
     *
     * @param pathToServerXml The path to the server.xml file to be examined
     * @param insertedStanza  the full stanza that is to be found
     */
    public static void validateStanzaInConfigFile(String pathToServerXml, String insertedStanza) {

        try {
            Assertions.assertTrue(isTextInFile(pathToServerXml, insertedStanza));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates the expected line of code has been inserted and is found
     *
     * @param pathToJavaSrc    The path to the server.xml file to be examined
     * @param insertedCodeLine the code string to verify
     */
    public static void validateCodeInJavaSrc(String pathToJavaSrc, String insertedCodeLine) {

        try {
            Assertions.assertTrue(isTextInFile(pathToJavaSrc, insertedCodeLine));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates the expected configuration name vale pair entry is found in config file
     *
     * @param pathToFile     The path to the config file to be examined
     * @param expectedString the full setting string that is to be found
     */
    public static void validateStringInFile(String pathToFile, String expectedString) {
        try {
            Assertions.assertTrue(isTextInFile(pathToFile, expectedString));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates the expected string is not found in the given file
     *
     * @param pathToFile     The path to the config file to be examined
     * @param expectedString the string that is expected not to be found
     */
    public static void validateStringNotInFile(String pathToFile, String expectedString) {
        try {
            Assertions.assertFalse(isTextInFile(pathToFile, expectedString));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * Validates that one of the test reports represented by the input paths exists.
     *
     * @param pathToTestReport34 The path to the report for maven-surefire-report-plugin 3.4 or earlier
     * @param pathToTestReport35 The path to the report for maven-surefire-report-plugin 3.5 or later
     */
    public static void validateTestReportExists(Path pathToTestReport34, Path pathToTestReport35) {
        int retryCountLimit = 100;
        int retryIntervalSecs = 1;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;

            boolean fileExists = fileExists(pathToTestReport34.toAbsolutePath()) || fileExists(pathToTestReport35.toAbsolutePath());
            if (!fileExists) {
                try {
                    Thread.sleep(retryIntervalSecs * 1000);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            } else {
                return;
            }
        }
        throw new IllegalStateException("Timed out waiting for test report: " + pathToTestReport34 + " or " + pathToTestReport35 + " file to be created.");
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
    public static boolean fileExists(Path filePath) {
        File f = new File(filePath.toString());
        return f.exists();
    }

    /**
     * Deletes file identified by the input path. If the file is a directory, it must be empty.
     *
     * @param path The path of the file to delete.
     * @return Returns true if the file identified by the input path was deleted. False, otherwise.
     */
    public static boolean deleteFile(Path path) {
        printTrace(TraceSevLevel.INFO, "deleteFile. Entry. Path: " + path);
        boolean deleted = true;
        File file = path.toFile();

        if (file.exists()) {
            if (!file.isDirectory()) {
                deleted = file.delete();
            } else {
                deleted = deleteDirectory(file);
            }
        }

        printTrace(TraceSevLevel.INFO, "deleteFile. Exit. Deleted: " + deleted);
        return deleted;
    }

    /**
     * Recursively deletes the input file directory.
     *
     * @param file The directory.
     * @return Returns true if the directory identified by the input path was deleted. False, otherwise.
     */
    public static boolean deleteDirectory(File file) {
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

    /**
     * Determines if the Liberty server should be stopped or not.
     *
     * @param wlpInstallPath The path to the Liberty installation.
     * @return True if the Liberty server should be stopped. False, otherwise.
     */
    public static boolean isServerStopNeeded(String wlpInstallPath) {
        boolean stopServer = false;
        Path msgLogPath = Paths.get(wlpInstallPath, WLP_MSGLOG_PATH.toString());
        if (fileExists(msgLogPath)) {
            try {
                // The file maybe an old log. For now, check for the message indicating
                // that the server is stopped.
                if (!(isTextInFile(msgLogPath.toString(), SEVER_STOPPED_MSG))) {
                    stopServer = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return stopServer;
    }

    /**
     * Checks if the debug port is set to the specified value in the server.env file.
     *
     * @param absoluteWLPPath The absolute path to the WLP directory.
     * @param debugPort The debug port to check in the server.env file.
     * @throws IOException If an I/O error occurs while reading the server.env file.
     */
    public static void checkDebugPort(String absoluteWLPPath, int debugPort) throws IOException {
        // Retrieve the WLP server.env file path
        Path serverEnvPath = Paths.get(absoluteWLPPath, "wlp", "usr", "servers", "defaultServer", "server.env");
        // Read all lines from server.env
        List<String> lines = Files.readAllLines(serverEnvPath);
        // Check if Debug Port is set to the specified port
        boolean debugPortIsSet = lines.stream().anyMatch(line -> line.contains("WLP_DEBUG_ADDRESS=" + debugPort));
        Assertions.assertTrue(debugPortIsSet, "Debug Port is not set to " + debugPort);
    }

    /**
     * Copies all files and directories from the source directory to the destination directory.
     *
     * @param sourceDirectoryLocation The path to the source directory to copy from.
     * @param destinationDirectoryLocation The path to the destination directory to copy to.
     * @throws IOException If an I/O error occurs while copying files.
     */
    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy file: " + source + " to " + destination, e);
                    }
                });
    }

    /**
     * Test to see if there has been a fatal error and JUnit should be stopped.
     * This searches the output of this JUnit run for SocketTimeoutException which
     * has been identified as a fatal error and occurs during the Mac tests.
     * It is set up as a static reader so that we do not reread the whole file
     * after each test.
     */
    final static String outputFile = System.getenv("JUNIT_OUTPUT_TXT");
    final static BufferedReader reader;

    static {
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void detectFatalError() {
        try {
            // Flush our output and then wait briefly for the process running 'tee' to flush output to the
            // file that the buffered reader is reading.
            System.out.flush();
            Thread.sleep(10);
            // Continue reading the existing reader
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("SocketTimeoutException")) {
                    System.exit(23);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
