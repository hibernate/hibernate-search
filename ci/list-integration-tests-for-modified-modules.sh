#!/bin/bash -e
# Lists test modules based on git diff check.

if (( $# < 2 ))
then
	echo 1>&2 "Usage: $0 <main branch, which we'll compare to to identify changes> <branch with changes we want to identify>"
	echo 1>&2 "Aborting."
	exit 1
fi

# `exec` will echo a based dir and the artifact id.
#   Basedir is going to be used to figure out to which module folder the modified files belong to
#   Artifact ID is going to be used to match the basedir to an id that can be later passed as a mvn build parameter
#
# The output format is `artifact-id-and-basedir-output :artifactId=basedir`.
# The prefix (`artifact-id-and-basedir-output`) is used for easier identification of the lines we are interested in from maven's output
readarray -t lines < <(./mvnw exec:exec@print-artifact-id-and-basedir | grep artifact-id-and-basedir-output | awk '{print $2}')

declare -a artifacts
declare -a paths

# We split the `:artifactId=basedir` and put them in their own arrays.
# Basedir will be used as patterns to determine which module the change belongs to.
for index in "${!lines[@]}"
do
	artifacts[index]=${lines[$index]%=*}
	paths[index]=${lines[$index]#*=}
	paths[index]="${paths[index]//${paths[0]}"/"/""}"
done

# get the diffs, which will produce relative paths of modified files:
readarray -t lines < <(git --no-pager diff --name-only $1..$2)

declare -A modifiedArtifactIds

# using an associated array as a set to have a unique collection of modified artifact IDs
for modifiedFile in "${lines[@]}"
do
  for index in "${!paths[@]}"
    do
      if [[ $modifiedFile =~ ${paths[$index]} ]]; then
          modifiedArtifactIds[${artifacts[$index]}]="modified"
      fi
    done
done

if [ "${#modifiedArtifactIds[@]}" -ne 0 ]; then
   $(dirname "$(realpath "$0")")/list-dependent-integration-tests.sh "${!modifiedArtifactIds[@]}"
fi

