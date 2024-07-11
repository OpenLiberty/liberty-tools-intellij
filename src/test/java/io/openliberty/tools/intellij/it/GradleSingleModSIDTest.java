/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Gradle project.
 */
public class GradleSingleModSIDTest extends SingleModMPSIDProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    private static final String PROJECTS_PATH_NEW = Paths.get("src", "test", "resources", "projects", "gradle-sample").toAbsolutePath().toString();

//    private final String PROJECTS_PATH_CHOOSE = Paths.get("src", "test", "resources", "projects", "gradle-sample").toAbsolutePath().toString();

    /**
     * Project port.
     */
    private final int SM_MP_PROJECT_PORT = 9080;

    /**
     * Project resource URI.
     */
    private final String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project response.
     */
    private final String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    private final String WLP_INSTALL_PATH = "build";

    /**
     * The path to the test report.
     */
    private final Path TEST_REPORT_PATH = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "build", "reports", "tests", "test", "index.html");

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() throws IOException {

        Path newDirPath = getDirectory(PROJECTS_PATH, "gradle-sample");
        TestUtils.sleepAndIgnoreException(10);
        prepareEnv(newDirPath.toString(), SM_MP_PROJECT_NAME);
    }

//    public static Path setupnew() throws IOException {
//
//        return getDirectory(PROJECTS_PATH);
//    }

    public static Path getDirectory(String path,String newDirName) throws IOException {
        Path oldDirPath = Paths.get(path);
        Path parentDirPath = oldDirPath.getParent();
//        String newDirName = newDirName;
        Path newDirPath = parentDirPath.resolve(newDirName);

        // Move all files and directories recursively
        boolean success = move(oldDirPath.toFile(), newDirPath.toFile());
        if (success) {
            // Delete the old directory if it's empty
            File index = new File(oldDirPath.toString());
            deleteDirectory(index);

//            TestUtils.sleepAndIgnoreException(60);
            Files.deleteIfExists(oldDirPath);
            System.out.println("Directory renamed successfully.");
        } else {
            System.err.println("Error renaming directory.");
        }
        return newDirPath;
    }

//    public static void deleteDirectory(File file)
//    {
//        // store all the paths of files and folders present
//        // inside directory
//        for (File subfile : file.listFiles()) {
//
//            // if it is a subfolder,e.g Rohan and Ritik,
//            //  recursively call function to empty subfolder
//            if (subfile.isDirectory()) {
//                deleteDirectory(subfile);
//            }
////            TestUtils.sleepAndIgnoreException(60);
////            TestUtils.sleepAndIgnoreException(60);
//            // delete files and empty subfolders
//            subfile.delete();
//        }
//    }

    private static boolean move(File sourceFile, File destFile) {
        if (sourceFile.isDirectory()) {
            destFile.mkdirs(); // Create the destination directory if it does not exist
            for (File file : sourceFile.listFiles()) {
                if (!move(file, new File(destFile, file.getName()))) {
                    return false; // Return false if moving any file or directory fails
                }
            }
        } else {
            try {
                Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return false; // Return false if moving the file fails
            }
        }
        return true; // Return true if moving the directory and all its contents succeeds
    }

    @AfterAll
    public static void setupPathToNormal() throws IOException {
//        Path newDirPath = getDirectory(PROJECTS_PATH_NEW, "gradle");

        Path oldDirPath = Paths.get(PROJECTS_PATH_NEW);
        Path parentDirPath = oldDirPath.getParent();
//        String newDirName = newDirName;
        Path newDirPath = parentDirPath.resolve("gradle");
        Path updatePath = oldDirPath.resolve("singleModGradleMP");
        File index = new File(oldDirPath.toString());

        // Move all files and directories recursively
        boolean success = move(oldDirPath.toFile(), newDirPath.toFile());
        if (success) {
            // Delete the old directory if it's empty
//            File index = new File(oldDirPath.toString());
//            deleteDirectory(index);
//            TestUtils.sleepAndIgnoreException(60);
//            Files.deleteIfExists(oldDirPath);
            System.out.println("Directory renamed successfully.");
        } else {
            System.err.println("Error renaming directory.");
        }
//         TestUtils.sleepAndIgnoreException(30);
//        if (updatePath.)
//        Files.deleteIfExists(updatePath);
//        Files.deleteIfExists(oldDirPath);
    }

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
    }

    /**
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    @Override
    public String getProjectsDirPath() {
        return PROJECTS_PATH;
    }

//    @Override
//    public String getProjectsDirPathNew() {
//        return PROJECTS_PATH_NEW;
//    }

    /**
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    @Override
    public String getSmMPProjectName() {
        return SM_MP_PROJECT_NAME;
    }

    /**
     * Returns the expected HTTP response payload associated with the single module
     * MicroProfile project.
     *
     * @return The expected HTTP response payload associated with the single module
     * MicroProfile project.
     */
    @Override
    public String getSmMPProjOutput() {
        return SM_MP_PROJECT_OUTPUT;
    }

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    @Override
    public int getSmMpProjPort() {
        return SM_MP_PROJECT_PORT;
    }

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    @Override
    public String getSmMpProjResURI() {
        return SM_MP_PROJECT_RES_URI;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean testReportDeleted = TestUtils.deleteFile(TEST_REPORT_PATH);
        Assertions.assertTrue(testReportDeleted, () -> "Test report file: " + TEST_REPORT_PATH + " was not be deleted.");
    }

}