# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in opensearch.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.5.0@sha256:919ff4e7d0d57dbc4bd0999ddf0e43e961bba844ec2a5b6734fc979eb4e32399
