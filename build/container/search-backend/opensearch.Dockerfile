# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in amazon-opensearch-serverless.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.10.0@sha256:02d6d6e40c820ae08233b60540aa5d3d9364d64e2dc724e38d6c8c794879b2a0
