package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.engine.common.timing.Deadline;

import tools.jackson.databind.node.ObjectNode;

public interface ElasticsearchSearchResultExtractor<R> {

	R extract(ObjectNode responseBody, Deadline deadline);
}
