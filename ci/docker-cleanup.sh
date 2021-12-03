#!/bin/bash -e

# Stop all running containers, so that we don't have problems with already allocated ports
# caused by previous builds failing to clean up after themselves.
docker ps -q | xargs -r docker stop

# Prune container-related resources. Mostly useful on CI to free up resources.
docker container prune -f || true
docker image prune -f --filter "until=24h" || true
docker network prune -f || true
docker volume prune -f || true
