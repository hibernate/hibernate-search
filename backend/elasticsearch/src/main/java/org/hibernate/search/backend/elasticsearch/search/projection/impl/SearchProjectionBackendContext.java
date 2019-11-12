/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

public class SearchProjectionBackendContext {

	private final DocumentReferenceExtractorHelper documentReferenceExtractorHelper;

	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper) {
		this.documentReferenceExtractorHelper = documentReferenceExtractorHelper;
	}

	ElasticsearchDocumentReferenceProjectionBuilder createDocumentReferenceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchDocumentReferenceProjectionBuilder( indexNames, documentReferenceExtractorHelper );
	}

	<E> ElasticsearchEntityProjectionBuilder<E> createEntityProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchEntityProjectionBuilder<>( indexNames, documentReferenceExtractorHelper );
	}

	<R> ElasticsearchEntityReferenceProjectionBuilder<R> createReferenceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchEntityReferenceProjectionBuilder<>( indexNames, documentReferenceExtractorHelper );
	}

	ElasticsearchScoreProjectionBuilder createScoreProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchScoreProjectionBuilder( indexNames );
	}

	ElasticsearchSourceProjectionBuilder createSourceProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchSourceProjectionBuilder( indexNames );
	}

	ElasticsearchExplanationProjectionBuilder createExplanationProjectionBuilder(Set<String> indexNames) {
		return new ElasticsearchExplanationProjectionBuilder( indexNames );
	}
}
