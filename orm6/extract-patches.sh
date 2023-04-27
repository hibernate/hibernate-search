#!/bin/bash -e

function log() {
  echo 1>&2 "${@}"
}

if ! [ -e ".git" ]
then
  log "This script must be executed from the git repository root"
  exit 1
fi

if (( $# != 1 ))
then
  log "Usage:"
  log "  $0 <git rev range>"
  exit 1
fi

REV_RANGE="$1"
shift

for subdir in $(cd orm6; grep -R --include pom.xml -L '<packaging>pom</packaging>' | xargs dirname)
do
  patchPath="orm6/$subdir/ant-src-changes.patch"
  git diff -p "$REV_RANGE" --relative="$subdir/src" -- "$subdir/src" ":^$subdir/src/main/asciidoc" > "$patchPath"
  if ! [ -s "$patchPath" ]
  then
    rm "$patchPath"
  fi
done
