/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.util.impl.CollectionHelper;

class ReferenceProjectionDocumentExtractor implements ProjectionDocumentExtractor {

	private static final ReferenceProjectionDocumentExtractor INSTANCE = new ReferenceProjectionDocumentExtractor();

	private static final Set<String> DOCUMENT_REFERENCE_FIELD_PATHS = CollectionHelper.asSet(
			LuceneFields.indexFieldName(),
			LuceneFields.idFieldName()
	);

	static ReferenceProjectionDocumentExtractor get() {
		return INSTANCE;
	}

	private ReferenceProjectionDocumentExtractor() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.addAll( DOCUMENT_REFERENCE_FIELD_PATHS );
	}

	@Override
	public void extract(ProjectionHitCollector collector, ScoreDoc scoreDoc, Document document) {
		DocumentReference documentReference = new LuceneDocumentReference(
				document.get( LuceneFields.indexFieldName() ),
				document.get( LuceneFields.idFieldName() )
		);

		collector.collectReference( documentReference );
	}
}
