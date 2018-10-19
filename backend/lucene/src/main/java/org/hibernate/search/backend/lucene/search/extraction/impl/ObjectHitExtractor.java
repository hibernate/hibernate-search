/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;

public class ObjectHitExtractor implements HitExtractor<LoadingHitCollector> {

	private static final ObjectHitExtractor INSTANCE = new ObjectHitExtractor();

	public static ObjectHitExtractor get() {
		return INSTANCE;
	}

	private ObjectHitExtractor() {
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
	public void extract(LoadingHitCollector collector, Document document, int docId, Float score) {
		collector.collectForLoading( DocumentReferenceExtractorHelper.extractDocumentReference( document ) );
	}
}
