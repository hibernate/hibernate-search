/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.search.DocumentReference;

public final class DocumentReferenceExtractorHelper {

	private DocumentReferenceExtractorHelper() {
	}

	public static void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	public static void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( LuceneFields.indexFieldName() );
		absoluteFieldPaths.add( LuceneFields.idFieldName() );
	}

	public static DocumentReference extractDocumentReference(Document document) {
		return new LuceneDocumentReference(
				document.get( LuceneFields.indexFieldName() ),
				document.get( LuceneFields.idFieldName() )
		);
	}
}
