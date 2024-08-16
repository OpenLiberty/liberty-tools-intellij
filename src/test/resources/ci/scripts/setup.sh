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

set -Eexo pipefail

# Current time.
currentTime=(date +"%Y/%m/%d-%H:%M:%S:%3N")

# Operating system.
OS=$(uname -s)

# Semeru JDK version control constants.
# Example:
# Version (SEMERU_OPEN_JDK_VERSION): 17.0.6
# OpenJDK (SEMERU_OPEN_JDK_VERSION + SEMERU_OPEN_JDK_BUILD): 17.0.6_10
# OpenJ9  (SEMERU_OPENJ9_VERSION): 0.36.0
SEMERU_OPEN_JDK_MAJOR=17
SEMERU_OPEN_JDK_VERSION="${SEMERU_OPEN_JDK_MAJOR}.0.6"
SEMERU_OPEN_JDK_BUILD=10
SEMERU_OPENJ9_VERSION=0.36.0

SEMERU_ARCHIVE_MAC_SHA256=37baae44a266c53a90e494be208564c690ed36b7b590f0d75e257efe9173e6c9
SEMERU_ARCHIVE_LINUX_SHA256=ce39a4f7c2e08e56083f17f3e44c05e0fbbeba775e670f015a337679c99c54c6
SEMERU_ARCHIVE_WINDOWS_SHA256=4143a9fe93b8a139be34f5789fe61bf197d772600b716fa4b3b8f09c0ab679da

# Maven version control constants.
MAVEN_VERSION=3.8.6
MAVEN_ARCHIVE_SHA512=f92dbd90060c5fd422349f844ea904a0918c9c9392f3277543ce2bfb0aab941950bb4174d9b6e2ea84cd48d2940111b83ffcc2e3acf5a5b2004277105fd22be9

# Gradle version control constants.
GRADLE_VERSION=7.6.1
GRADLE_ARCHIVE_SHA256=6147605a23b4eff6c334927a86ff3508cb5d6722cd624c97ded4c2e8640f1f87

# Software install directory.
SOFTWARE_INSTALL_DIR="${PWD}/test-tools/liberty-dev-tools"

# main.
# If accepts a single argument representing the custom installation function to call.
# The argument can be one of these options:
# installJDK, installMaven, installGradle, or installBaseSoftware.
# If no argument is specified all software is installed.
main() {
  local funcToCall="$1"

    # If we are not on a supported OS, exit.
    if [[ $OS != "Linux" && $OS != "Darwin" && $OS != "MINGW64_NT"* ]]; then
        echo "ERROR: OS $OS is not supported."
        exit -1
    fi

    # Create install directory.
    mkdir -p "$SOFTWARE_INSTALL_DIR"

    # Install software.
    case "$funcToCall" in
        installJDK)
            installJDK
        ;;
        installMaven)
            installMaven
        ;;
        installGradle)
            installGradle
        ;;
        installBaseSoftware)
            installBaseSoftware
        ;;
        *)
            installBaseSoftware
            installCustomSoftware
        ;;
    esac
}

# installSoftware installs base software.
# MAC: Add `|| true` to failing brew command to bypass issue:
# https://github.com/actions/setup-python/issues/577
# Once the issue is resolved, `|| true' can be removed.
installBaseSoftware() {
    if [[ $OS == "Linux" ]]; then
        sudo apt-get update
        sudo apt-get -y install curl unzip
        installXDisplaySoftwareOnLinux
        installDockerOnLinux
    elif [[ $OS == "Darwin" ]]; then
        brew update
        brew install --quiet curl unzip || true
        # installDockerOnMAC
    else
        # Note: Docker is already installed on the windows VMs provisioned by GHA.
        # Location: C:\Program Files\Docker\dockerd.exe
        choco install curl
        choco install unzip
    fi
}

# installSoftware installs customizable software.
installCustomSoftware() {
	installJDK
	installMaven
	installGradle
}

# installJDK installs the set version of the Semeru JDK.
installJDK() {
	local javaHome="${SOFTWARE_INSTALL_DIR}/jdk-${SEMERU_OPEN_JDK_VERSION}+${SEMERU_OPEN_JDK_BUILD}"

    # Download, validate, and expand the JDK archive.
	if [[ $OS == "Linux" ]]; then
        local url="https://github.com/ibmruntimes/semeru${SEMERU_OPEN_JDK_MAJOR}-binaries/releases/download/jdk-${SEMERU_OPEN_JDK_VERSION}%2B${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}/ibm-semeru-open-jdk_x64_linux_${SEMERU_OPEN_JDK_VERSION}_${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}.tar.gz"
        curl -fsSL -o /tmp/liberty-dev-tool-semeru-jdk.tar.gz "$url"
        echo "${SEMERU_ARCHIVE_LINUX_SHA256}  /tmp/liberty-dev-tool-semeru-jdk.tar.gz" | sha256sum -c -
        tar -xzf /tmp/liberty-dev-tool-semeru-jdk.tar.gz -C "$SOFTWARE_INSTALL_DIR"
    elif [[ $OS == "Darwin" ]]; then
       javaHome="$javaHome"/Contents/Home
       local url="https://github.com/ibmruntimes/semeru${SEMERU_OPEN_JDK_MAJOR}-binaries/releases/download/jdk-${SEMERU_OPEN_JDK_VERSION}%2B${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}/ibm-semeru-open-jdk_x64_mac_${SEMERU_OPEN_JDK_VERSION}_${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}.tar.gz"
       curl -fsSL -o /tmp/liberty-dev-tool-semeru-jdk.tar.gz "$url"
       echo "${SEMERU_ARCHIVE_MAC_SHA256}  /tmp/liberty-dev-tool-semeru-jdk.tar.gz" | shasum -a 256 -c -
       tar -xzf /tmp/liberty-dev-tool-semeru-jdk.tar.gz -C "$SOFTWARE_INSTALL_DIR"
    else
        local url="https://github.com/ibmruntimes/semeru${SEMERU_OPEN_JDK_MAJOR}-binaries/releases/download/jdk-${SEMERU_OPEN_JDK_VERSION}%2B${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}/ibm-semeru-open-jdk_x64_windows_${SEMERU_OPEN_JDK_VERSION}_${SEMERU_OPEN_JDK_BUILD}_openj9-${SEMERU_OPENJ9_VERSION}.zip"
        curl -fsSL -o /tmp/liberty-dev-tool-semeru-jdk.zip "$url"
        local shaAll=$(certutil -hashfile /tmp/liberty-dev-tool-semeru-jdk.zip SHA256)
        local downloadedZipSha=$(echo $(echo $shaAll | tr '\r' ' ') | cut -d " " -f 5)
        if [ "$SEMERU_ARCHIVE_WINDOWS_SHA256" != "$downloadedZipSha" ]; then
            echo "ERROR: expected SHA: $SEMERU_ARCHIVE_WINDOWS_SHA256 is not equal to downloaded file calculated SHA of: $downloadedZipSha"
            exit -1
        fi
        unzip /tmp/liberty-dev-tool-semeru-jdk.zip -d "$SOFTWARE_INSTALL_DIR"
    fi

    # Handle Java home.
    if [[ $GITHUB_ENV ]]; then
        echo "JAVA_HOME=${javaHome}" >> $GITHUB_ENV
        echo "${javaHome}/bin" >> $GITHUB_PATH
    else
        echo "${javaHome}"
    fi
}

# installMaven installs the set version of Maven.
installMaven() {
    local mavenHome="${SOFTWARE_INSTALL_DIR}/apache-maven-${MAVEN_VERSION}"
	local url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip"

    # Download the Maven archive.
	curl -fsSL -o /tmp/liberty-dev-tool-apache-maven.zip "$url"

	# Check the downloaded archive's SHA against the expected value.
	if [[ $OS == "Linux" ]]; then
        echo "${MAVEN_ARCHIVE_SHA512}  /tmp/liberty-dev-tool-apache-maven.zip" | sha512sum -c -
    elif [[ $OS == "Darwin" ]]; then
        echo "${MAVEN_ARCHIVE_SHA512}  /tmp/liberty-dev-tool-apache-maven.zip" | shasum -a 512 -c -
    else
        local shaAll=$(certutil -hashfile /tmp/liberty-dev-tool-apache-maven.zip SHA512)
        local downloadedZipSha=$(echo $(echo $shaAll | tr '\r' ' ') | cut -d " " -f 5)
        if [ "$MAVEN_ARCHIVE_SHA512" != "$downloadedZipSha" ]; then
            echo "ERROR: expected SHA: $MAVEN_ARCHIVE_SHA512 is not equal to downloaded file calculated SHA of: $downloadedZipSha"
            exit -1
        fi
    fi

    # Expand the archive.
    unzip -d "$SOFTWARE_INSTALL_DIR" /tmp/liberty-dev-tool-apache-maven.zip

    # Handle Maven home.
    if [[ $GITHUB_PATH ]]; then
        echo "${mavenHome}/bin" >> $GITHUB_PATH
    else
        echo "${mavenHome}"
    fi
}

# installGradle installs the set version of Gradle.
installGradle() {
    local gradleHome="${SOFTWARE_INSTALL_DIR}/gradle-${GRADLE_VERSION}"
	local url="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

    # Download the Gradle archive.
    curl -fsSL -o /tmp/liberty-dev-tool-gradle.zip "$url"

    # Check the downloaded archive's SHA against the expected value.
	if [[ $OS == "Linux" ]]; then
        echo "${GRADLE_ARCHIVE_SHA256}  /tmp/liberty-dev-tool-gradle.zip" | sha256sum -c -
    elif [[ $OS == "Darwin" ]]; then
        echo "${GRADLE_ARCHIVE_SHA256}  /tmp/liberty-dev-tool-gradle.zip" | shasum -a 256 -c -
    else
        local shaAll=$(certutil -hashfile /tmp/liberty-dev-tool-gradle.zip SHA256)
        local downloadedZipSha=$(echo $(echo $shaAll | tr '\r' ' ') | cut -d " " -f 5)
        if [ "$GRADLE_ARCHIVE_SHA256" != "$downloadedZipSha" ]; then
            echo "ERROR: expected SHA: $GRADLE_ARCHIVE_SHA256 is not equal to downloaded file calculated SHA of: $downloadedZipSha"
            exit -1
        fi
    fi

    # Expand the archive.
    unzip -d "$SOFTWARE_INSTALL_DIR" /tmp/liberty-dev-tool-gradle.zip

    # Handle Gradle home.
    if [[ $GITHUB_PATH ]]; then
        echo "${gradleHome}/bin" >> $GITHUB_PATH
    else
        echo "${gradleHome}"
    fi
}

# installXDisplaySoftwareOnLinux Installs a X display, a windows manager, and other pre-req software.
installXDisplaySoftwareOnLinux() {
    sudo DEBIAN_FRONTEND=noninteractive apt-get -y install dbus-x11 xvfb metacity at-spi2-core
}

# installDockerOnLinux installs Docker on Linux.
installDockerOnLinux() {
    # Remove a previous installation of docker.
    for i in docker docker-engine docker.io containerd runc; do
        if isPkgInstalled $i; then
            sudo apt-get -y remove $i
        fi
    done

    # Setup the docker repository before installation.
    sudo apt-get -y install ca-certificates gnupg lsb-release
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
       $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install the docker engine.
    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
}

# installDockerOnMAC installs Docker on MAC.
installDockerOnMAC() {
        # Download and Install Docker desktop.
        curl -O https://desktop.docker.com/mac/main/amd64/Docker.dmg
        sudo hdiutil attach Docker.dmg
        sudo /Volumes/Docker/Docker.app/Contents/MacOS/install --accept-license --user runner
        sudo hdiutil detach /Volumes/Docker

        # Start Docker desktop.
        open -a /Applications/Docker.app --args --unattended

        # Wait for start to complete.
        dockerDaemonRunning=$(docker info &> /dev/null || true)
        local iterations=0
        local maxIterations=60
        while [[ -z "$dockerDaemonRunning" && "$iterations" -lt "$maxIterations" ]]; do
            echo -e "\n$(${currentTime[@]}): Docker daemon is not running. Waiting ..."
            let iterations+=1
            sleep 5
            dockerDaemonRunning=$(docker info &> /dev/null || true)
        done

        # Sanity check. This will either show the info output of a successful start.
        # or will fail the setup on an unsuccessful start.
        docker info
}

# Returns 0 if the package is installed.
isPkgInstalled() {
  dpkg --status "$1" &> /dev/null
}

main "$@"
