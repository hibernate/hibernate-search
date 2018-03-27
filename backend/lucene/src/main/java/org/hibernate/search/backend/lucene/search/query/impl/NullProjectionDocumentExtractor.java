/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

class NullProjectionDocumentExtractor implements ProjectionDocumentExtractor {

	private static final NullProjectionDocumentExtractor INSTANCE = new NullProjectionDocumentExtractor();

	public static NullProjectionDocumentExtractor get() {
		return INSTANCE;
	}

	private NullProjectionDocumentExtractor() {
		// Private constructor, use get() instead
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
	}

	@Override
	public void extract(ProjectionHitCollector collector, ScoreDoc scoreDoc, Document document) {
		collector.collectProjection( null );
	}
}
