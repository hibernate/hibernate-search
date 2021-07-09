/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.IdentifierCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

public class LuceneIdProjection<I> extends AbstractLuceneProjection<String, I> {

	private final DocumentIdentifierValueConverter<? extends I> identifierValueConverter;

	private LuceneIdProjection(LuceneSearchIndexScope<?> scope,
			DocumentIdentifierValueConverter<? extends I> identifierValueConverter) {
		super( scope );
		this.identifierValueConverter = identifierValueConverter;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireCollector( IdentifierCollector.FACTORY );
	}

	@SuppressWarnings("unchecked")
	@Override
	public String extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return context.getCollector( IdentifierCollector.KEY ).get( documentResult.getDocId() );
	}

	@Override
	public I transform(LoadingResult<?, ?> loadingResult, String extractedData,
			SearchProjectionTransformContext context) {
		return identifierValueConverter.convertToSource(
				extractedData, context.fromDocumentIdentifierValueConvertContext() );
	}

	public static class Builder<I> extends AbstractBuilder<I> implements IdProjectionBuilder<I> {

		private final LuceneIdProjection<I> projection;

		public Builder(LuceneSearchIndexScope<?> scope, Class<I> identifierType) {
			super( scope );

			DocumentIdentifierValueConverter<?> identifierValueConverter =
					scope.identifier().dslConverter( ValueConvert.YES );

			// check expected identifier type:
			identifierValueConverter.checkSourceTypeAssignableTo( identifierType );
			@SuppressWarnings("uncheked") // just checked
			DocumentIdentifierValueConverter<? extends I> casted = (DocumentIdentifierValueConverter<? extends I>) identifierValueConverter;

			projection = new LuceneIdProjection<>( scope, casted );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
