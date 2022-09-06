#!/bin/bash -e

if (( $# < 1 ))
then
	echo 1>&2 "Usage: $0 <name of update> <comma-separated properties to update>"
	echo 1>&2 "See the settings() method in ci/dependency-update/Jenkinsfile for valid values."
	exit 1
fi

DEPENDENCY_UPDATE_NAME="$1"
shift
PROPERTIES="$1"
shift

: ${BRANCH_NAME:="$(git branch --show-current)"}

git switch --detach

BRANCH_TO_MERGE="origin/wip/${BRANCH_NAME}/dependency-update/${DEPENDENCY_UPDATE_NAME}"
if git rev-parse --quiet --verify "${BRANCH_TO_MERGE}"
then
	echo "WIP branch found; merging."
	git merge --no-edit "${BRANCH_TO_MERGE}"
	echo "Merge complete."
else
	echo "No WIP branch found; skipping merge."
fi

if [ -n "$PROPERTIES" ]
then
	echo "Updating properties '$PROPERTIES'."
	# We allow any update, but constrain using a rules file (more flexible)
	./mvnw --quiet -U -Pdependency-update -Pdist org.codehaus.mojo:versions-maven-plugin:update-properties \
		-DgenerateBackupPoms=false \
		-DallowMajorUpdates=true -DallowMinorUpdates=true -DallowSnapshots=true \
		-DincludeProperties="$PROPERTIES" \
		-Dmaven.version.rules="file://$(pwd)/ci/dependency-update/rules-$DEPENDENCY_UPDATE_NAME.xml" \
		"${@}"
	echo "Property update complete."
	echo 'Updated version properties:'
	git --no-pager diff HEAD
else
	echo "No properties to update."
fi
