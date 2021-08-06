#!/bin/bash -e
# Prune container-related resources. Mostly useful on CI to free up resources.

docker container prune -f || true
docker image prune -f --filter "until=24h" || true
docker network prune -f || true
docker volume prune -f || true
