/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

class IndexSensitiveHitExtractor<C> implements HitExtractor<C> {

	private final Map<String, HitExtractor<? super C>> extractorByIndex;

	IndexSensitiveHitExtractor(Map<String, HitExtractor<? super C>> extractorByIndex) {
		this.extractorByIndex = extractorByIndex;
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( LuceneFields.indexFieldName() );

		for ( HitExtractor<?> extractor : extractorByIndex.values() ) {
			extractor.contributeFields( absoluteFieldPaths );
		}
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( HitExtractor<?> extractor : extractorByIndex.values() ) {
			extractor.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void extract(C collector, Document document) {
		String indexName = document.get( LuceneFields.indexFieldName() );
		HitExtractor<? super C> delegate = extractorByIndex.get( indexName );
		delegate.extract( collector, document );
	}
}
