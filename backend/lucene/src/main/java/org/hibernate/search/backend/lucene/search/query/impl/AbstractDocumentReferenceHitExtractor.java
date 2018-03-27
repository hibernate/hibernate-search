/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.backend.lucene.search.impl.LuceneDocumentReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.util.impl.CollectionHelper;

abstract class AbstractDocumentReferenceHitExtractor<T> implements HitExtractor<T> {

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor = new ReusableDocumentStoredFieldVisitor(
			CollectionHelper.asSet( LuceneFields.idFieldName(), LuceneFields.indexFieldName() )
	);

	protected AbstractDocumentReferenceHitExtractor() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	protected DocumentReference extractDocumentReference(IndexSearcher indexSearcher, int documentId) throws IOException {
		indexSearcher.doc( documentId, storedFieldVisitor );
		Document document = storedFieldVisitor.getDocumentAndReset();
		DocumentReference documentReference = new LuceneDocumentReference(
				document.get( LuceneFields.indexFieldName() ),
				document.get( LuceneFields.idFieldName() )
		);
		return documentReference;
	}
}
