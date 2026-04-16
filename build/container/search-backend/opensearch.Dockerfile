# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in amazon-opensearch-serverless.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.6.0@sha256:57bd3c879ad27123a9a6cd75e2adba504189d3131d00a669f3baf9210bc4538c
