#!/bin/bash -e
# Lists integration test projects that depend on a given project.
# The output format is the one expected by `mvn -pl`.

if (( $# < 1 ))
then
	echo 1>&2 "Usage: $0 <artifactIds that listed IT modules must depend on> [<other artifactIds that listed IT modules must depend on (at least one)> ...]"
	echo 1>&2 "Aborting."
	exit 1
fi

IFS="," COMMA_SEPARATED_TESTED_ARTIFACT_IDS="$*"
TESTED_ARTIFACT_IDS_REGEXP="(\Q$(echo "$*" | perl -pe 's/,/\\E|\\Q/g')\E)"

# We don't want to use the build cache or build scans for this execution.
# -Ddevelocity.scan.disabled disables scan capture entirely (not just publication),
# preventing scan data from being saved and later published by the CI reporting workflow.
MISC_MAVEN_ARGS="-Ddevelocity.scan.disabled=true -Dno-build-cache"

{
	./mvnw dependency:list $MISC_MAVEN_ARGS -pl :hibernate-search-parent-integrationtest -am -amd -DincludeArtifactIds="$COMMA_SEPARATED_TESTED_ARTIFACT_IDS"
} \
	| perl -0777 -pe "s/(hibernate-search-.*) ---(\n(?!.*Building).*)*\n.*:$TESTED_ARTIFACT_IDS_REGEXP:jar:/\nMATCH:\$1\n/g" \
	| perl -ne 'print if s/MATCH:(.*)/:$1/g' \
	| perl -0777 -pe 's/\n(?!$)/,/g'
