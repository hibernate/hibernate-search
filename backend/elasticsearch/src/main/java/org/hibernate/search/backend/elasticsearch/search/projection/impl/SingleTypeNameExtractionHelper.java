/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
