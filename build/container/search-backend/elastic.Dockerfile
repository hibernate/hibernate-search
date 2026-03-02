# Elasticsearch
# https://hub.docker.com/r/elastic/elasticsearch/tags
#
# IMPORTANT! When updating the version of Elasticsearch in this Dockerfile, make sure to
#  * update `version.org.elasticsearch.latest` property in a POM file.
#  * update the tags for 'elasticsearch-current' and 'elasticsearch-next' builds in ci/dependency-update/Jenkinsfile
#
FROM docker.io/elastic/elasticsearch:9.3.1@sha256:cb637acad8808664b7c57455a2b2ee87e30b3cab210f3ccc423ff6bcc305e0b9
