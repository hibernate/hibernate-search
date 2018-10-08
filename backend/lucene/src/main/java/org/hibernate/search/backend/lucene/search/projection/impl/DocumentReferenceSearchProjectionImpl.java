/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class DocumentReferenceSearchProjectionImpl implements LuceneSearchProjection<DocumentReference> {

	private static final DocumentReferenceSearchProjectionImpl INSTANCE = new DocumentReferenceSearchProjectionImpl();

	static DocumentReferenceSearchProjectionImpl get() {
		return INSTANCE;
	}

	private DocumentReferenceSearchProjectionImpl() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( LuceneFields.indexFieldName() );
		absoluteFieldPaths.add( LuceneFields.idFieldName() );
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, int docId, Float score) {
		collector.collectProjection( DocumentReferenceExtractorHelper.extractDocumentReference( document ) );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
