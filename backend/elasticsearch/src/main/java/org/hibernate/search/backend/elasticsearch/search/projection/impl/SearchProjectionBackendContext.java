/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.extraction.impl.DocumentReferenceExtractorHelper;

public class SearchProjectionBackendContext {

	private final DocumentReferenceSearchProjectionBuilderImpl documentReferenceProjectionBuilder;
	private final ObjectSearchProjectionBuilderImpl objectProjectionBuilder;
	private final ReferenceSearchProjectionBuilderImpl referenceProjectionBuilder;
	private final ScoreSearchProjectionBuilderImpl scoreProjectionBuilder;

	public SearchProjectionBackendContext(DocumentReferenceExtractorHelper documentReferenceExtractorHelper) {
		this.documentReferenceProjectionBuilder = new DocumentReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.objectProjectionBuilder = new ObjectSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.referenceProjectionBuilder = new ReferenceSearchProjectionBuilderImpl( documentReferenceExtractorHelper );
		this.scoreProjectionBuilder = new ScoreSearchProjectionBuilderImpl();
	}

	public DocumentReferenceSearchProjectionBuilderImpl getDocumentReferenceProjectionBuilder() {
		return documentReferenceProjectionBuilder;
	}

	public ObjectSearchProjectionBuilderImpl getObjectProjectionBuilder() {
		return objectProjectionBuilder;
	}

	public ReferenceSearchProjectionBuilderImpl getReferenceProjectionBuilder() {
		return referenceProjectionBuilder;
	}

	public ScoreSearchProjectionBuilderImpl getScoreProjectionBuilder() {
		return scoreProjectionBuilder;
	}
}
