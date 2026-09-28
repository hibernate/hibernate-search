package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import com.google.gson.JsonObject;

public interface ProjectionExtractionHelper<T> {

	void request(JsonObject requestBody, ProjectionRequestContext context);

	T extract(JsonObject hit, ProjectionExtractContext context);

}
