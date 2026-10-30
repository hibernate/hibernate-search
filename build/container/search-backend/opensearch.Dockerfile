# OpenSearch
# See https://hub.docker.com/r/opensearchproject/opensearch/tags
#
# IMPORTANT! When updating the version of OpenSearch in this Dockerfile,
# make sure to update `version.org.opensearch.latest` property in a POM file,
# and to update the version in amazon-opensearch-serverless.Dockerfile as well.
FROM docker.io/opensearchproject/opensearch:3.9.0@sha256:3b55f6b6249cfdb92b52ee59adc8a266203ba9ef9f5d82544fad6f1af2f3609e
