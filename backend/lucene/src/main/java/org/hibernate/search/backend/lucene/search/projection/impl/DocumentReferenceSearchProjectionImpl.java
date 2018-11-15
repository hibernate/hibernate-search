/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

class DocumentReferenceSearchProjectionImpl implements LuceneSearchProjection<DocumentReference> {

	private static final DocumentReferenceSearchProjectionImpl INSTANCE = new DocumentReferenceSearchProjectionImpl();

	static DocumentReferenceSearchProjectionImpl get() {
		return INSTANCE;
	}

	private DocumentReferenceSearchProjectionImpl() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		DocumentReferenceExtractorHelper.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		DocumentReferenceExtractorHelper.contributeFields( absoluteFieldPaths );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExecutionContext context) {
		return DocumentReferenceExtractorHelper.extractDocumentReference( documentResult );
	}

	@Override
	public DocumentReference transform(LoadingResult<?> loadingResult, Object extractedData) {
		return (DocumentReference) extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
