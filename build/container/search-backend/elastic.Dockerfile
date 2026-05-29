# Elasticsearch
# https://hub.docker.com/r/elastic/elasticsearch/tags
#
# IMPORTANT! When updating the version of Elasticsearch in this Dockerfile, make sure to
#  * update `version.org.elasticsearch.latest` property in a POM file.
#  * update the tags for 'elasticsearch-current' and 'elasticsearch-next' builds in ci/dependency-update/Jenkinsfile
#
FROM docker.io/elastic/elasticsearch:9.4.2@sha256:be5f49784ff5ec8a5b5d7ba17f944d9d6b10c067f596ee93e6b6cb82d2dd874c
