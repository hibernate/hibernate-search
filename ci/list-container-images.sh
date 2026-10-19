#!/bin/bash -e
# Lists container images required for a build with the given Maven arguments (enabled profiles and Maven properties)

LIST_FILE=$(mktemp)
trap "rm -f $LIST_FILE" EXIT

# We don't want to use the build cache or build scans for this execution.
# Use standard Develocity system properties rather than custom conventions:
# - develocity.scan.disabled disables scan capture entirely (not just publication),
#   preventing scan data from being saved and later published by the CI reporting workflow.
# - develocity.cache.{local,remote}.enabled disables the build cache.
MISC_MAVEN_ARGS="-Ddevelocity.scan.disabled=true -Ddevelocity.cache.local.enabled=false -Ddevelocity.cache.remote.enabled=false"

# See the configuration of the maven-scripting-plugin in the main POM
./mvnw scripting:eval@list-container-images $MISC_MAVEN_ARGS -DcontainerImagesListFile="$LIST_FILE" "${@}" 1>&2

sort -u "$LIST_FILE" | { grep -Ev "^\s*$" || true; }
