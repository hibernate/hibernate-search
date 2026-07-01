# Elasticsearch
# https://hub.docker.com/r/elastic/elasticsearch/tags
#
# IMPORTANT! When updating the version of Elasticsearch in this Dockerfile, make sure to
#  * update `version.org.elasticsearch.latest` property in a POM file.
#  * update the tags for 'elasticsearch-current' and 'elasticsearch-next' builds in ci/dependency-update/Jenkinsfile
#
FROM docker.io/elastic/elasticsearch:9.4.3@sha256:851ff5f9615ab7d2f00931114f6db32850f0208ec9fe7e841f135ac78f5f13d5
