/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

class LuceneDocumentReferenceProjection extends AbstractLuceneProjection<DocumentReference>
		implements LuceneSearchProjection.Extractor<DocumentReference, DocumentReference> {

	LuceneDocumentReferenceProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, DocumentReference> request(ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.DOCUMENT_REFERENCE,
				LuceneSearchHints.INSTANCE.documentReferenceProjectionNestingNotSupportedHint()
		);
		return this;
	}

	@Override
	public Values<DocumentReference> values(ProjectionExtractContext context) {
		return DocumentReferenceValues.simple( context.collectorExecutionContext() );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}
}
