/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFields;
import org.hibernate.search.util.impl.CollectionHelper;

class IndexSensitiveHitExtractor<C> implements HitExtractor<C> {

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor = new ReusableDocumentStoredFieldVisitor(
			CollectionHelper.asSet( LuceneFields.indexFieldName() )
	);

	private final Map<String, HitExtractor<? super C>> extractorByIndex;

	IndexSensitiveHitExtractor(Map<String, HitExtractor<? super C>> extractorByIndex) {
		this.extractorByIndex = extractorByIndex;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( HitExtractor<?> extractor : extractorByIndex.values() ) {
			extractor.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void extract(C collector, IndexSearcher indexSearcher, ScoreDoc scoreDoc) throws IOException {
		indexSearcher.doc( scoreDoc.doc, storedFieldVisitor );
		Document document = storedFieldVisitor.getDocumentAndReset();
		String indexName = document.get( LuceneFields.indexFieldName() );
		HitExtractor<? super C> delegate = extractorByIndex.get( indexName );
		delegate.extract( collector, indexSearcher, scoreDoc );
	}
}
