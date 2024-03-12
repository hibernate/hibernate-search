# Ryuk
# See https://hub.docker.com/r/testcontainers/ryuk/tags
#
# IMPORTANT! When updating the version for Ryuk in this Dockerfile,
# make sure to update `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` env variable set as part of maven-failsafe-plugin configuration.
#
FROM docker.io/testcontainers/ryuk:0.7.0