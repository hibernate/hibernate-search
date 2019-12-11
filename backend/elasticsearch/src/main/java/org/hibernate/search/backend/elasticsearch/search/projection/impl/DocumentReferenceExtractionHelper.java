/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchDocumentReference;
import org.hibernate.search.engine.backend.common.DocumentReference;

import com.google.gson.JsonObject;

public final class DocumentReferenceExtractionHelper implements ProjectionExtractionHelper<DocumentReference> {
	private final ProjectionExtractionHelper<String> mappedTypeNameHelper;
	private final ProjectionExtractionHelper<String> idHelper;

	public DocumentReferenceExtractionHelper(ProjectionExtractionHelper<String> mappedTypeNameHelper,
			ProjectionExtractionHelper<String> idHelper) {
		this.mappedTypeNameHelper = mappedTypeNameHelper;
		this.idHelper = idHelper;
	}

	@Override
	public void request(JsonObject requestBody) {
		mappedTypeNameHelper.request( requestBody );
		idHelper.request( requestBody );
	}

	@Override
	public DocumentReference extract(JsonObject hit) {
		String mappedTypeName = mappedTypeNameHelper.extract( hit );
		String id = idHelper.extract( hit );
		return new ElasticsearchDocumentReference( mappedTypeName, id );
	}
}
