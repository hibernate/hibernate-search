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
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;

import org.apache.lucene.search.Explanation;

class LuceneExplanationProjection extends AbstractLuceneProjection<Explanation, Explanation> {

	private LuceneExplanationProjection(LuceneSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		// We do not need anything specific.
	}

	@Override
	public Explanation extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return context.explain( documentResult.getDocId() );
	}

	@Override
	public Explanation transform(LoadingResult<?, ?> loadingResult, Explanation extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public static class Builder extends AbstractLuceneProjection.AbstractBuilder<Explanation>
			implements SearchProjectionBuilder<Explanation> {

		public Builder(LuceneSearchContext searchContext) {
			super( searchContext );
		}

		@Override
		public SearchProjection<Explanation> build() {
			return new LuceneExplanationProjection( searchContext );
		}
	}
}
