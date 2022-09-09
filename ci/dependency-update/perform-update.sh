#!/bin/bash -e

function find_single_rev() {
	local list
	list="$(git rev-parse --verify --symbolic "${@}" 2>/dev/null || true)"
	case $(echo "$list" | wc -l) in
		0)
			return
			;;
	  1)
			echo -n $list
			;;
		*)
			echo 1>&2 "Found multiple revs matching '${*}':"
			echo 1>&2 "$list"
			echo 1>&2 "If you know which one you want to use, try to check out that branch locally, then switch back to branch '$BRANCH_NAME' and try again."
			echo 1>&2 "Aborting."
			exit 1
			;;
	esac
}

if (( $# < 1 ))
then
	echo 1>&2 "Usage: $0 <name of update> <comma-separated properties to update>"
	echo 1>&2 "See the settings() method in ci/dependency-update/Jenkinsfile for valid values."
	echo 1>&2 "Aborting."
	exit 1
fi

DEPENDENCY_UPDATE_NAME="$1"
shift
if (( $# >= 1 ))
then
	PROPERTIES="$1"
	shift
fi

: ${BRANCH_NAME:="$(git branch --show-current)"}

WIP_BRANCH_NAME="wip/${BRANCH_NAME}/dependency-update/${DEPENDENCY_UPDATE_NAME}"
echo "Searching for WIP branch '$WIP_BRANCH_NAME'..."
REV_TO_MERGE="$(find_single_rev "${WIP_BRANCH_NAME}")"
if [ -n "$REV_TO_MERGE" ]
then
	echo "Found local WIP branch: '$REV_TO_MERGE'"
else
	REV_TO_MERGE="$(find_single_rev --remotes="*/${WIP_BRANCH_NAME}")"
	if [ -n "$REV_TO_MERGE" ]
	then
		echo "Found remote-tracking WIP branch: '$REV_TO_MERGE'"
	else
		echo "No WIP branch found (neither local nor remote-tracking)."
	fi
fi

echo "Detaching HEAD..."
git switch --detach

if [ -n "$REV_TO_MERGE" ]
then
	echo "Merging '$REV_TO_MERGE'..."
	git merge --no-edit "$REV_TO_MERGE"
	echo "Merge complete."
else
	echo "Skipping merge."
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
