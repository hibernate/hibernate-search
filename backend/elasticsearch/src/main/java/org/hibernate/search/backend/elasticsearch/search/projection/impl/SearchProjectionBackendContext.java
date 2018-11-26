/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

public class SearchProjectionBackendContext {

	private final ElasticsearchDocumentReferenceProjectionBuilder documentReferenceProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ElasticsearchObjectProjectionBuilder objectProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ElasticsearchReferenceProjectionBuilder referenceProjectionBuilder;
	private final ElasticsearchScoreProjectionBuilder scoreProjectionBuilder;

	@SuppressWarnings("rawtypes")
	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper) {
		this.documentReferenceProjectionBuilder = new ElasticsearchDocumentReferenceProjectionBuilder( documentReferenceExtractorHelper );
		this.objectProjectionBuilder = new ElasticsearchObjectProjectionBuilder( documentReferenceExtractorHelper );
		this.referenceProjectionBuilder = new ElasticsearchReferenceProjectionBuilder( documentReferenceExtractorHelper );
		this.scoreProjectionBuilder = new ElasticsearchScoreProjectionBuilder();
	}

	ElasticsearchDocumentReferenceProjectionBuilder getDocumentReferenceProjectionBuilder() {
		return documentReferenceProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<O> ElasticsearchObjectProjectionBuilder<O> getObjectProjectionBuilder() {
		return objectProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<R> ElasticsearchReferenceProjectionBuilder<R> getReferenceProjectionBuilder() {
		return referenceProjectionBuilder;
	}

	ElasticsearchScoreProjectionBuilder getScoreProjectionBuilder() {
		return scoreProjectionBuilder;
	}
}
