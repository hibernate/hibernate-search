/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.engine.backend.common.DocumentReference;

public final class DocumentReferenceExtractorHelper {

	private DocumentReferenceExtractorHelper() {
	}

	public static void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
		luceneCollectorBuilder.addCollector( DocumentReferenceCollector.FACTORY );
	}

	public static void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		// No-op
	}

	public static DocumentReference extractDocumentReference(SearchProjectionExtractContext context,
			LuceneResult documentResult) {
		return context.getCollector( DocumentReferenceCollector.FACTORY ).get( documentResult.getDocId() );
	}
}
