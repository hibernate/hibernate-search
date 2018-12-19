/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

class LuceneScoreProjection implements LuceneSearchProjection<Float, Float> {

	private static final LuceneScoreProjection INSTANCE = new LuceneScoreProjection();

	static LuceneScoreProjection get() {
		return INSTANCE;
	}

	private LuceneScoreProjection() {
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		// Nothing to contribute
	}

	@Override
	public Float extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return documentResult.getScore();
	}

	@Override
	public Float transform(LoadingResult<?> loadingResult, Float extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
