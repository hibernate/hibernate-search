#!/bin/bash -e
# Lists container images required for a build with the given Maven arguments (enabled profiles and Maven properties)

LIST_FILE=$(mktemp)
trap "rm -f $LIST_FILE" EXIT

# We don't want to use the build cache or build scans for this execution
MISC_MAVEN_ARGS="-Dscan=false -Dno-build-cache"

# See the configuration of the maven-scripting-plugin in the main POM
./mvnw scripting:eval@list-container-images $MISC_MAVEN_ARGS -DcontainerImagesListFile="$LIST_FILE" "${@}" 1>&2

sort -u "$LIST_FILE" | { grep -Ev "^\s*$" || true; }
