/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import com.google.gson.Gson;

public class SearchProjectionBackendContext {

	private final DocumentReferenceExtractorHelper documentReferenceExtractorHelper;
	private final Gson userFacingGson;

	@SuppressWarnings("rawtypes")
	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper,
			Gson userFacingGson) {
		this.documentReferenceExtractorHelper = documentReferenceExtractorHelper;
		this.userFacingGson = userFacingGson;
	}

	ElasticsearchDocumentReferenceProjectionBuilder createDocumentReferenceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchDocumentReferenceProjectionBuilder( indexNames, documentReferenceExtractorHelper );
	}

	<E> ElasticsearchEntityProjectionBuilder<E> createEntityProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchEntityProjectionBuilder<>( indexNames, documentReferenceExtractorHelper );
	}

	<R> ElasticsearchReferenceProjectionBuilder<R> createReferenceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchReferenceProjectionBuilder<>( indexNames, documentReferenceExtractorHelper );
	}

	ElasticsearchScoreProjectionBuilder createScoreProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchScoreProjectionBuilder( indexNames );
	}

	ElasticsearchSourceProjectionBuilder createSourceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchSourceProjectionBuilder( indexNames, userFacingGson );
	}

	public SearchProjectionBuilder<String> createExplanationProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchExplanationProjectionBuilder( indexNames, userFacingGson );
	}
}
