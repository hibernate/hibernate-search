/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;

class LuceneDocumentReferenceProjection extends AbstractLuceneProjection<DocumentReference, DocumentReference> {

	private LuceneDocumentReferenceProjection(LuceneSearchIndexScope scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireCollector( DocumentReferenceCollector.FACTORY );
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return context.getCollector( DocumentReferenceCollector.KEY ).get( documentResult.getDocId() );
	}

	@Override
	public DocumentReference transform(LoadingResult<?, ?> loadingResult, DocumentReference extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	public static class Builder extends AbstractLuceneProjection.AbstractBuilder<DocumentReference>
			implements DocumentReferenceProjectionBuilder {

		public Builder(LuceneSearchIndexScope scope) {
			super( scope );
		}

		@Override
		public SearchProjection<DocumentReference> build() {
			return new LuceneDocumentReferenceProjection( scope );
		}
	}
}
