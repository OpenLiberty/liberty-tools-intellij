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
    echo -e "\n> $(${currentTime[@]}): Build: Building the plugin"

    # Tell the terminal session to use display port 77.
    export DISPLAY=:77.0

    # Install software.
    if [ $OS = "Linux" ]; then
        # Start the X display server on port 77.
        Xvfb -ac :77 -screen 0 1280x1024x16 > /dev/null 2>&1 &

        #  Start the window manager.
        metacity --sm-disable --replace 2> metacity.err &
    fi

    # Run the plugin's test task
    ./gradlew clean runIdeForUiTests& ./gradlew test

    # If there were any errors, gather some debug data before exiting.
    rc=$?
    if [ "$rc" -ne 0 ]; then
        echo "ERROR: Failure while driving mvn install on plugin. rc: ${rc}."

        echo "DEBUG: Liberty messages.log:\n"
        cat tests/resources/applications/maven/liberty-maven-test-app/target/liberty/wlp/usr/servers/defaultServer/logs/messages.log

        echo "DEBUG: Environment variables:\n"
        env

        exit -1
    fi
}

main "$@"
