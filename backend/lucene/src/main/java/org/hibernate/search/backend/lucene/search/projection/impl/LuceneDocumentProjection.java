/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import org.apache.lucene.document.Document;

class LuceneDocumentProjection implements LuceneSearchProjection<Document, Document> {

	private final Set<String> indexNames;

	public LuceneDocumentProjection(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireAllStoredFields();
	}

	@Override
	public Document extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return documentResult.getDocument();
	}

	@Override
	public Document transform(LoadingResult<?> loadingResult, Document extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
