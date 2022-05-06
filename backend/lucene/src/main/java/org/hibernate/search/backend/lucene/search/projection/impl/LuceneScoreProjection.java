/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.ScoreValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

class LuceneScoreProjection extends AbstractLuceneProjection<Float>
		implements LuceneSearchProjection.Extractor<Float, Float> {

	LuceneScoreProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, Float> request(ProjectionRequestContext context) {
		context.requireScore();
		return this;
	}

	@Override
	public Values<Float> values(ProjectionExtractContext context) {
		return new ScoreValues( context.collectorExecutionContext() );
	}

	@Override
	public Float transform(LoadingResult<?, ?> loadingResult, Float extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}
}
