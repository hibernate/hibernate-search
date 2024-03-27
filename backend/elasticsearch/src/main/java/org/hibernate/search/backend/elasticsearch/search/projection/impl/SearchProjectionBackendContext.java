/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;

public final class SearchProjectionBackendContext {

	private final ProjectionExtractionHelper<String> complexMappedTypeNameProjectionExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public SearchProjectionBackendContext(
			ProjectionExtractionHelper<String> complexMappedTypeNameProjectionExtractionHelper,
			ProjectionExtractionHelper<String> idProjectionExtractionHelper) {
		this.complexMappedTypeNameProjectionExtractionHelper = complexMappedTypeNameProjectionExtractionHelper;
		this.idProjectionExtractionHelper = idProjectionExtractionHelper;
	}

	ProjectionExtractionHelper<String> createMappedTypeNameExtractionHelper(ElasticsearchSearchIndexScope<?> scope) {
		Set<String> mappedTypeNames = scope.mappedTypeNameToIndex().keySet();
		if ( mappedTypeNames.size() == 1 ) {
			// Only one type targeted by the search: use a simpler implementation that will always work.
			return new SingleTypeNameExtractionHelper( mappedTypeNames.iterator().next() );
		}
		else {
			return complexMappedTypeNameProjectionExtractionHelper;
		}
	}

	DocumentReferenceExtractionHelper createDocumentReferenceExtractionHelper(
			ProjectionExtractionHelper<String> mappedTypeNameExtractionHelper) {
		return new DocumentReferenceExtractionHelper( mappedTypeNameExtractionHelper, idProjectionExtractionHelper );
	}

	public ProjectionExtractionHelper<String> idProjectionExtractionHelper() {
		return idProjectionExtractionHelper;
	}
}
