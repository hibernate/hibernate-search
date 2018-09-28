/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class ScoreHitExtractor implements HitExtractor<ProjectionHitCollector> {

	private static final ScoreHitExtractor INSTANCE = new ScoreHitExtractor();

	private ScoreHitExtractor() {
	}

	public static ScoreHitExtractor get() {
		return INSTANCE;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, Float score) {
		collector.collectProjection( score );
	}
}
