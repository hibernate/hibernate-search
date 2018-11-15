/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

public class SearchProjectionBackendContext {

	private final DocumentReferenceSearchProjectionBuilderImpl documentReferenceProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ObjectSearchProjectionBuilderImpl objectProjectionBuilder;
	@SuppressWarnings("rawtypes")
	private final ReferenceSearchProjectionBuilderImpl referenceProjectionBuilder;
	private final ScoreSearchProjectionBuilderImpl scoreProjectionBuilder;

	@SuppressWarnings("rawtypes")
	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper) {
		this.documentReferenceProjectionBuilder = new DocumentReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.objectProjectionBuilder = new ObjectSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.referenceProjectionBuilder = new ReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.scoreProjectionBuilder = new ScoreSearchProjectionBuilderImpl();
	}

	DocumentReferenceSearchProjectionBuilderImpl getDocumentReferenceProjectionBuilder() {
		return documentReferenceProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<O> ObjectSearchProjectionBuilderImpl<O> getObjectProjectionBuilder() {
		return objectProjectionBuilder;
	}

	@SuppressWarnings("unchecked")
	<R> ReferenceSearchProjectionBuilderImpl<R> getReferenceProjectionBuilder() {
		return referenceProjectionBuilder;
	}

	ScoreSearchProjectionBuilderImpl getScoreProjectionBuilder() {
		return scoreProjectionBuilder;
	}
}
