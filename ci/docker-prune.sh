#!/bin/bash -e
# Prune container-related resources. Mostly useful on CI to free up resources.

docker container prune -f || true
docker image prune -f || true
docker network prune -f || true
docker volume prune -f || true
