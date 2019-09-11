/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.backend.common.DocumentReference;

public final class DocumentReferenceExtractorHelper {

	private DocumentReferenceExtractorHelper() {
	}

	public static void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	public static void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		builder.add( LuceneFields.indexFieldName() );
		builder.add( LuceneFields.idFieldName() );
	}

	public static DocumentReference extractDocumentReference(LuceneResult documentResult) {
		return new LuceneDocumentReference(
				documentResult.getStringValue( LuceneFields.indexFieldName() ),
				documentResult.getStringValue( LuceneFields.idFieldName() )
		);
	}
}
