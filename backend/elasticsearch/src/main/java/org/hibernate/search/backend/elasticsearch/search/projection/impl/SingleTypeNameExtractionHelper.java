/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import com.google.gson.JsonObject;

public final class SingleTypeNameExtractionHelper implements ProjectionExtractionHelper<String> {

	private final String mappedTypeName;

	public SingleTypeNameExtractionHelper(String mappedTypeName) {
		this.mappedTypeName = mappedTypeName;
	}

	@Override
	public void request(JsonObject requestBody, ProjectionRequestContext context) {
		// Nothing to do
	}

	@Override
	public String extract(JsonObject hit, ProjectionExtractContext context) {
		return mappedTypeName;
	}
}
