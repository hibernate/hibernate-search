package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import tools.jackson.databind.node.ObjectNode;

public interface ProjectionExtractionHelper<T> {

	void request(ObjectNode requestBody, ProjectionRequestContext context);

	T extract(ObjectNode hit, ProjectionExtractContext context);

}
