#!/bin/bash -e
# Lists container images required for a build with the given Maven arguments (enabled profiles and Maven properties)

LIST_FILE=$(mktemp)
trap "rm -f $LIST_FILE" EXIT

# See the configuration of the maven-scripting-plugin in the main POM
./mvnw scripting:eval@list-container-images -DcontainerImagesListFile="$LIST_FILE" "${@}" 1>&2

sort -u "$LIST_FILE" | { grep -Ev "^\s*$" || true; }
