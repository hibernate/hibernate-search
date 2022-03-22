/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;

class LuceneDocumentProjection extends AbstractLuceneProjection<Document>
		implements LuceneSearchProjection.Extractor<Document, Document> {

	private LuceneDocumentProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, Document> request(ProjectionRequestContext context) {
		context.requireAllStoredFields();
		return this;
	}

	@Override
	public Values<Document> values(ProjectionExtractContext context) {
		StoredFieldsValuesDelegate delegate = context.collectorExecutionContext().storedFieldsValuesDelegate();
		return new Values<Document>() {
			@Override
			public void context(LeafReaderContext context) {
				// Nothing to do
			}

			@Override
			public Document get(int doc) {
				return delegate.get( doc );
			}
		};
	}

	@Override
	public Document transform(LoadingResult<?, ?> loadingResult, Document extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}

	public static class Builder extends AbstractLuceneProjection.AbstractBuilder<Document>
			implements SearchProjectionBuilder<Document> {

		public Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchProjection<Document> build() {
			return new LuceneDocumentProjection( scope );
		}
	}
}
