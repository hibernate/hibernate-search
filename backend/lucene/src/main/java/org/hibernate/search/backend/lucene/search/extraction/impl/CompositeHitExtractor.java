/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.List;
import java.util.Set;

/**
 * A hit extractor used when multiple values must be extracted for each hit.
 * <p>
 * Used for projections.
 */
public class CompositeHitExtractor<C> implements HitExtractor<C> {

	private final List<HitExtractor<? super C>> extractors;

	public CompositeHitExtractor(List<HitExtractor<? super C>> extractors) {
		this.extractors = extractors;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( HitExtractor<? super C> extractor : extractors ) {
			extractor.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		for ( HitExtractor<? super C> extractor : extractors ) {
			extractor.contributeFields( absoluteFieldPaths );
		}
	}

	@Override
	public void extract(C collector, LuceneResult documentResult) {
		for ( HitExtractor<? super C> extractor : extractors ) {
			extractor.extract( collector, documentResult );
		}
	}
}
