/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class LuceneDocumentReferenceProjection implements LuceneSearchProjection<DocumentReference, DocumentReference> {

	private static final LuceneDocumentReferenceProjection INSTANCE = new LuceneDocumentReferenceProjection();

	static LuceneDocumentReferenceProjection get() {
		return INSTANCE;
	}

	private LuceneDocumentReferenceProjection() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		DocumentReferenceExtractorHelper.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		DocumentReferenceExtractorHelper.contributeFields( builder );
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return DocumentReferenceExtractorHelper.extractDocumentReference( documentResult );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
