/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

class ScoreSearchProjectionImpl implements LuceneSearchProjection<Float> {

	private static final ScoreSearchProjectionImpl INSTANCE = new ScoreSearchProjectionImpl();

	static ScoreSearchProjectionImpl get() {
		return INSTANCE;
	}

	private ScoreSearchProjectionImpl() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		// Nothing to contribute
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, int docId, Float score) {
		collector.collectProjection( score );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
