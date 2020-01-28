/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;

public final class SearchProjectionBackendContext {

	private final ProjectionExtractionHelper<String> complexMappedTypeNameProjectionExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public SearchProjectionBackendContext(
			ProjectionExtractionHelper<String> complexMappedTypeNameProjectionExtractionHelper,
			ProjectionExtractionHelper<String> idProjectionExtractionHelper) {
		this.complexMappedTypeNameProjectionExtractionHelper = complexMappedTypeNameProjectionExtractionHelper;
		this.idProjectionExtractionHelper = idProjectionExtractionHelper;
	}

	DocumentReferenceExtractionHelper createDocumentReferenceExtractionHelper(ElasticsearchSearchContext context) {
		Set<String> mappedTypeNames = context.getMappedTypeNames();
		ProjectionExtractionHelper<String> mappedTypeNameHelper;
		if ( mappedTypeNames.size() == 1 ) {
			// Only one type targeted by the search: use a simpler implementation that will always work.
			mappedTypeNameHelper = new SingleTypeNameExtractionHelper( context.getMappedTypeNames().iterator().next() );
		}
		else {
			mappedTypeNameHelper = complexMappedTypeNameProjectionExtractionHelper;
		}
		return new DocumentReferenceExtractionHelper( mappedTypeNameHelper, idProjectionExtractionHelper );
	}

}
