package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import tools.jackson.databind.node.ObjectNode;

public final class SingleTypeNameExtractionHelper implements ProjectionExtractionHelper<String> {

	private final String mappedTypeName;

	public SingleTypeNameExtractionHelper(String mappedTypeName) {
		this.mappedTypeName = mappedTypeName;
	}

	@Override
	public void request(ObjectNode requestBody, ProjectionRequestContext context) {
		// Nothing to do
	}

	@Override
	public String extract(ObjectNode hit, ProjectionExtractContext context) {
		return mappedTypeName;
	}
}
