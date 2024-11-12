#!/bin/bash

############################################################################
# Copyright (c) 2023, 2024 IBM Corporation and others.
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

# Primes the environment before running the tests.
# This decreases the possibility of intermittent test issues
# related to varying dependency download times the first time a test is
# run. Performance will be improved once issue:
# https://github.com/OpenLiberty/ci.maven/issues/1557 is resolved.
#
# @parameter1: The working directory.
prefetchDependencies() {
    local workingDir="$1"

    # Go to the working dir.
    cd "$workingDir"

    # Build the product to prime for product dependencies.
    ./gradlew build -x test

    # Run through dev mode server install/create and feature installation for the Maven app.
    cd "src/test/resources/projects/maven/singleModMavenMP"
    ./mvnw liberty:install-server -ntp
    ./mvnw liberty:create
    ./mvnw liberty:install-feature -ntp

    # Run through dev mode server install/create and feature installation for the Gradle app.
    cd "$workingDir"
    cd "src/test/resources/projects/gradle/singleModGradleMP"
    ./gradlew installLiberty
    ./gradlew libertyCreate
    ./gradlew installFeature

    # Go back to the working dir.
    cd "$workingDir"
}

# Gathers resource usage information.
gatherResourceUsageData() {
    df -h
    if [[ "$OS" == MINGW* ]]; then
        systeminfo
        ps -ef
    elif [[ "$OS" == Linux* ]]; then
        free
        ps -efv
    elif [[ "$OS" == Darwin* ]]; then
        memory_pressure
        ps -efv
    fi
}

# Gathers logs and environmental information. The logs can be found in the <OS>-test-report.zip
# archive under the "Artifacts" section of the GHA actions build page.
#
# @parameter1: The working directory.
gatherDebugData() {
    local workingDir="$1"

    echo -e "DEBUG: Creating a build/reports directory if one does not already exist...\n"
    if [ ! -d "$workingDir/build/reports" ]; then
        mkdir -p "$workingDir"/build/reports
    fi

    echo -e "DEBUG: Gathering IDE remote server logs...\n"
    if [ -f "$workingDir/remoteServer.log" ]; then
        cp "$workingDir"/remoteServer.log "$workingDir"/build/reports/.
    fi

    echo -e "DEBUG: Gathering videos...\n"
    if [ -d "$workingDir/video" ]; then
        mv "$workingDir"/video "$workingDir"/build/reports/.
    fi

    echo -e "DEBUG: Gathering the IDEA log...\n"
    if [ -f "$workingDir/build/idea-sandbox/system/log/idea.log" ]; then
        cp "$workingDir"/build/idea-sandbox/system/log/idea.log "$workingDir"/build/reports/.
    fi

    echo -e "DEBUG: Gathering JVM crash log...\n"
    if [ $(ls "$workingDir"/hs_err_*.log* 2> /dev/null) ]; then
        cp "$workingDir"/hs_err_*.log "$workingDir"/build/reports/.
    fi

    echo -e "DEBUG: Installed Java version:\n"
    java -version

    echo -e "DEBUG: Installed Mvn version:\n"
    mvn -version

    echo -e "DEBUG: Installed Gradle version:\n"
    ./gradlew -version

    echo -e "DEBUG: Environment variables:\n"
    env

    echo -e "DEBUG: Resource usage and process information:\n"
    gatherResourceUsageData
}

# Start the IDE and wait for it to initialize. If the IDE takes too long this routine
# will exit the script with error code 12.
startIDE() {
      # Start the IDE.
      echo -e "\n$(${currentTime[@]}): INFO: Starting the IntelliJ IDE..."
      # Have liberty tools debugger wait 480s for Maven or Gradle dev mode to start
      export LIBERTY_TOOLS_INTELLIJ_DEBUGGER_TIMEOUT=480
      ./gradlew runIdeForUiTests -PuseLocal=$USE_LOCAL_PLUGIN --info  > remoteServer.log  2>&1 &

      # Wait for the IDE to come up.
      echo -e "\n$(${currentTime[@]}): INFO: Waiting for the Intellij IDE to start..."
      callLivenessEndpoint=(curl -s http://localhost:8082)
      count=1
      while ! ${callLivenessEndpoint[@]} | grep -qF 'Welcome to IntelliJ IDEA'; do
          if [ $count -eq 24 ]; then
              echo -e "\n$(${currentTime[@]}): ERROR: Timed out waiting for the Intellij IDE Welcome Page to start. Output:"
              exit 12
          fi
          count=`expr $count + 1`
          echo -e "\n$(${currentTime[@]}): INFO: Continue waiting for the Intellij IDE to start..." && sleep 5
      done
}

# Runs UI tests and collects debug data.
main() {
    echo -e "\n$(${currentTime[@]}): INFO: Gathering environment and resource usage data prior to test run..."
    id
    gatherResourceUsageData

    echo -e "\n$(${currentTime[@]}): INFO: Starting integration test run..."
    local currentLoc=$(pwd)

    # Add some env properties to .bashrc. This prevents cases (i.e. MAC) in which the IDE's
    # terminal does not recognize the JAVA_HOME environment variable set prior to the IDE starting.
    echo "export JAVA_HOME=$JAVA_HOME" >> $HOME/.bashrc
    echo "export PATH=$PATH:\$PATH" >> $HOME/.bashrc
    cat $HOME/.bashrc

    # Tell the terminal session to use display port 77.
    export DISPLAY=:77.0

    # Manage the display on Linux.
    if [ "$OS" = "Linux" ]; then
        # Start the X display server on port 77.
        Xvfb -ac :77 -screen 0 1920x1080x24 > /dev/null 2>&1 &
        sleep 10

        #  Start the window manager.
        metacity --sm-disable --replace 2> metacity.err &
    fi

    # Clean the project.
    ./gradlew clean

    # Prime the environment.
    echo -e "\n$(${currentTime[@]}): INFO: Prefetching dependencies..."
    prefetchDependencies "$currentLoc"
    rc=$?
    if [ "$rc" -ne 0 ]; then
        echo -e "\n$(${currentTime[@]}): ERROR: Failure while priming the env. rc: ${rc}."
        exit 11
    fi

    # Retry the tests if they fail with a SocketTimeoutException up to three times
    for retry in {1..3}; do
        startIDE
        # Run the tests
        echo -e "\n$(${currentTime[@]}): INFO: Running tests..."
        set -o pipefail # using tee requires we use this setting to gather the rc of gradlew
        ./gradlew test -PuseLocal=$USE_LOCAL_PLUGIN | tee "$currentLoc"/build/junit.out
        testRC=$?
        set +o pipefail # reset this option
        grep -i "SocketTimeoutException" "$currentLoc"/build/junit.out
        rc=$?
        if [ "$rc" -ne 0 ]; then
            # rc=1 means the exception string was not found
            break
        fi
    done

    # If there were any errors, gather some debug data before exiting.
    if [ "$testRC" -ne 0 ]; then
        echo -e "\n$(${currentTime[@]}): ERROR: Failure while running tests. rc: ${$testRC}."
        gatherDebugData "$currentLoc"
        exit -1
    fi
}

main "$@"
