/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;

class LuceneScoreProjection extends AbstractLuceneProjection<Float, Float> {

	private LuceneScoreProjection(LuceneSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireScore();
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

	public static class Builder extends AbstractLuceneProjection.AbstractBuilder<Float>
			implements ScoreProjectionBuilder {

		public Builder(LuceneSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public SearchProjection<Float> build() {
			return new LuceneScoreProjection( searchContext );
		}
	}
}
