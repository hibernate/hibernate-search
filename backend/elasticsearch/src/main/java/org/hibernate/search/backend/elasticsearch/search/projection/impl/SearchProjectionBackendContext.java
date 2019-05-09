/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import com.google.gson.Gson;

public class SearchProjectionBackendContext {

	private final ElasticsearchDocumentReferenceProjectionBuilder documentReferenceProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ElasticsearchEntityProjectionBuilder objectProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ElasticsearchReferenceProjectionBuilder referenceProjectionBuilder;
	private final ElasticsearchScoreProjectionBuilder scoreProjectionBuilder;
	private final ElasticsearchSourceProjectionBuilder sourceProjectionBuilder;
	private final ElasticsearchExplanationProjectionBuilder explanationProjectionBuilder;

	@SuppressWarnings("rawtypes")
	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper,
			Gson userFacingGson) {
		this.documentReferenceProjectionBuilder = new ElasticsearchDocumentReferenceProjectionBuilder( documentReferenceExtractorHelper );
		this.objectProjectionBuilder = new ElasticsearchEntityProjectionBuilder( documentReferenceExtractorHelper );
		this.referenceProjectionBuilder = new ElasticsearchReferenceProjectionBuilder( documentReferenceExtractorHelper );
		this.scoreProjectionBuilder = new ElasticsearchScoreProjectionBuilder();
		this.sourceProjectionBuilder = new ElasticsearchSourceProjectionBuilder( userFacingGson );
		this.explanationProjectionBuilder = new ElasticsearchExplanationProjectionBuilder( userFacingGson );
	}

	ElasticsearchDocumentReferenceProjectionBuilder getDocumentReferenceProjectionBuilder() {
		return documentReferenceProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<E> ElasticsearchEntityProjectionBuilder<E> getEntityProjectionBuilder() {
		return objectProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<R> ElasticsearchReferenceProjectionBuilder<R> getReferenceProjectionBuilder() {
		return referenceProjectionBuilder;
	}

	ElasticsearchScoreProjectionBuilder getScoreProjectionBuilder() {
		return scoreProjectionBuilder;
	}

	ElasticsearchSourceProjectionBuilder getSourceProjectionBuilder() {
		return sourceProjectionBuilder;
	}

	public SearchProjectionBuilder<String> getExplanationProjectionBuilder() {
		return explanationProjectionBuilder;
	}
}
