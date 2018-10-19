/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Set;

import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;

public class ReferenceHitExtractor implements HitExtractor<ReferenceHitCollector> {

	private static final ReferenceHitExtractor INSTANCE = new ReferenceHitExtractor();

	public static ReferenceHitExtractor get() {
		return INSTANCE;
	}

	private ReferenceHitExtractor() {
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
	public void extract(ReferenceHitCollector collector, LuceneResult documentResult) {
		collector.collectReference( DocumentReferenceExtractorHelper.extractDocumentReference( documentResult ) );
	}
}
