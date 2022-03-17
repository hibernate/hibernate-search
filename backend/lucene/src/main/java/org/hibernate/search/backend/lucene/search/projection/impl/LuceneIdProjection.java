/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.IdentifierValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

public class LuceneIdProjection<I> extends AbstractLuceneProjection<I>
		implements LuceneSearchProjection.Extractor<String, I> {

	private final ProjectionConverter<String, I> converter;

	private LuceneIdProjection(LuceneSearchIndexScope<?> scope, ProjectionConverter<String, I> converter) {
		super( scope );
		this.converter = converter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, I> request(ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Values<String> values(ProjectionExtractContext context) {
		return new IdentifierValues();
	}

	@Override
	public I transform(LoadingResult<?, ?> loadingResult, String extractedData,
			ProjectionTransformContext context) {
		return converter.fromDocumentValue( extractedData, context.fromDocumentValueConvertContext() );
	}

	public static class Builder<I> extends AbstractBuilder<I> implements IdProjectionBuilder<I> {

		private final LuceneIdProjection<I> projection;

		public Builder(LuceneSearchIndexScope<?> scope, ProjectionConverter<String, I> converter) {
			super( scope );
			projection = new LuceneIdProjection<>( scope, converter );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
