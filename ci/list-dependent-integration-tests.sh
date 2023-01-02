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

{
	./mvnw -Pdist dependency:list -pl :hibernate-search-parent-integrationtest -am -amd -DincludeArtifactIds="$COMMA_SEPARATED_TESTED_ARTIFACT_IDS"
	./mvnw -Pdist dependency:list -pl :hibernate-search-parent-integrationtest-orm6 -am -amd -DincludeArtifactIds="$COMMA_SEPARATED_TESTED_ARTIFACT_IDS"
	./mvnw -Pdist dependency:list -pl :hibernate-search-parent-integrationtest-jakarta -am -amd -DincludeArtifactIds="$COMMA_SEPARATED_TESTED_ARTIFACT_IDS"
} \
	| perl -0777 -pe "s/(hibernate-search-.*) ---(\n(?!.*Building).*)*\n.*:$TESTED_ARTIFACT_IDS_REGEXP:jar:/\nMATCH:\$1\n/g" \
	| perl -ne 'print if s/MATCH:(.*)/:$1/g' \
	| perl -0777 -pe 's/\n(?!$)/,/g'