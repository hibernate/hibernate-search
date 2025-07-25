# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in amazon-opensearch-serverless.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.1.0
