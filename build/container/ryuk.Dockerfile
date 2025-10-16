# Ryuk
# See https://hub.docker.com/r/testcontainers/ryuk/tags
#
# IMPORTANT! When updating the version for Ryuk in this Dockerfile,
# make sure to update `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` env variable set as part of maven-failsafe-plugin configuration.
#
# 0.14.0
FROM docker.io/testcontainers/ryuk@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0
