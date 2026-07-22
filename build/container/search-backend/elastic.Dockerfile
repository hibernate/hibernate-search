# Elasticsearch
# https://hub.docker.com/r/elastic/elasticsearch/tags
#
# IMPORTANT! When updating the version of Elasticsearch in this Dockerfile, make sure to
#  * update `version.org.elasticsearch.latest` property in a POM file.
#  * update the tags for 'elasticsearch-current' and 'elasticsearch-next' builds in ci/dependency-update/Jenkinsfile
#
FROM docker.io/elastic/elasticsearch:9.4.4@sha256:a3545404e436e348721786bed638042a81f2181ebe2e2636501bb43940677747
