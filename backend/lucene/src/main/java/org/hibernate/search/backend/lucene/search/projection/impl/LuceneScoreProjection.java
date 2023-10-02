/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.ScoreValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

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
		context.checkNotNested(
				ProjectionTypeKeys.SCORE,
				LuceneSearchHints.INSTANCE.scoreProjectionNestingNotSupportedHint()
		);
		context.requireScore();
		return this;
	}

	@Override
	public Values<Float> values(ProjectionExtractContext context) {
		return new ScoreValues( context.collectorExecutionContext() );
	}

	@Override
	public Float transform(LoadingResult<?> loadingResult, Float extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}
}
