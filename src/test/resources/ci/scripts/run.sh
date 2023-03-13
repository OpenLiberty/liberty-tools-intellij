#!/bin/bash

############################################################################
# Copyright (c) 2023 IBM Corporation and others.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# SPDX-License-Identifier: EPL-2.0
############################################################################
set -Ex

# Current time.
currentTime=(date +"%Y/%m/%d-%H:%M:%S:%3N")

# Operating system.
OS=$(uname -s)

main() {
    echo -e "\n$(${currentTime[@]}): INFO: Starting integration test run."

    # Tell the terminal session to use display port 77.
    export DISPLAY=:77.0

    # Manage the display on Linux.
    if [ $OS = "Linux" ]; then
        # Start the X display server on port 77.
        Xvfb -ac :77 -screen 0 1920x1080x24 > /dev/null 2>&1 &
        sleep 10

        #  Start the window manager.
        metacity --sm-disable --replace 2> metacity.err &
    fi

    # Create a directory to store the logs.
    currentLoc=$(pwd)
    mkdir -p "$currentLoc"/build/reports/tests

    # Clean the project.
    ./gradlew clean

    # Build the product. This avoids dependency download related timing issues and speeds up testing.
    ./gradlew build -x test

    # Run IDEA.
    ./gradlew runIdeForUiTests --info  > remoteServer.log  2>&1 &

    # WAIT for the IDE to come up.
    echo -e "\n$(${currentTime[@]}): INFO: Waiting for the test IDE to start."
    callLivenessEndpoint=(curl -s http://localhost:8082)
    count=1
    while ! ${callLivenessEndpoint[@]} | grep -qF 'Welcome to IntelliJ IDEA'; do
        if [ $count -eq 24 ]; then
            echo -e "\n$(${currentTime[@]}): ERROR: Timed out waiting for the Intellij IDE Welcome Page to start. Output:"
            cat "$currentLoc"/remoteServer.log
            exit 12
        fi
        count=`expr $count + 1`
        echo -e "\n$(${currentTime[@]}): INFO: Waiting for the test IDE to start..." && sleep 5
        done

    # Run the tests
    echo -e "\n$(${currentTime[@]}): INFO: Running tests..."
    ./gradlew test

    # If there were any errors, gather some debug data before exiting.
    rc=$?
    if [ "$rc" -ne 0 ]; then
        echo -e "\n$(${currentTime[@]}): ERROR: Failure while running tests. rc: ${rc}."

        echo -e "DEBUG: Collecting IDE remote server logs...\n"
        if [ -f "$currentLoc/remoteServer.log" ]; then
            cp "$currentLoc"/remoteServer.log "$currentLoc"/build/reports/.
        fi

        echo -e "DEBUG: Collecting videos...\n"
        if [ -d "$currentLoc/video" ]; then
            mv "$currentLoc"/video "$currentLoc"/build/reports/.
        fi

        echo -e "DEBUG: Collecting IDEA log...\n"
        if [ -f "$currentLoc/build/idea-sandbox/system/log/idea.log" ]; then
            cp "$currentLoc"/build/idea-sandbox/system/log/idea.log "$currentLoc"/build/reports/.
        fi

        echo -e "DEBUG: MVN version:\n"
        mvn -version

        echo -e "DEBUG: Gradle version:\n"
        ./gradlew -version

        echo -e "DEBUG: Environment variables:\n"
        env

        exit -1
    fi
}

main "$@"
