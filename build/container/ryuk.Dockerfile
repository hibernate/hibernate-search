# Ryuk
# See https://hub.docker.com/r/testcontainers/ryuk/tags
#
# IMPORTANT! When updating the version for Ryuk in this Dockerfile,
# make sure to update `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` env variable set as part of maven-failsafe-plugin configuration.
#
# 0.14.0
FROM docker.io/testcontainers/ryuk@sha256:b2762871bff62df9bfcec62609da821803a575e06d74aa7ce1654ded208cc7c5
