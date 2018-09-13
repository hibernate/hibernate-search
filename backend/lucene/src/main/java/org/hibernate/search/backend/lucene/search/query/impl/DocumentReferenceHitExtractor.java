/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.apache.lucene.document.Document;
import org.hibernate.search.engine.search.query.spi.DocumentReferenceHitCollector;

public class DocumentReferenceHitExtractor extends AbstractDocumentReferenceHitExtractor<DocumentReferenceHitCollector> {

	private static final DocumentReferenceHitExtractor INSTANCE = new DocumentReferenceHitExtractor();

	public static DocumentReferenceHitExtractor get() {
		return INSTANCE;
	}

	private DocumentReferenceHitExtractor() {
	}

	@Override
	public void extract(DocumentReferenceHitCollector collector, Document document) {
		collector.collectReference( extractDocumentReference( document ) );
	}
}
