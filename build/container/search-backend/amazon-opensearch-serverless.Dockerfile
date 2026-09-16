# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in opensearch.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.8.0@sha256:fafe3fc3587088674669235575aa166228c48bdb940294a8cdbbc1da75236a40
