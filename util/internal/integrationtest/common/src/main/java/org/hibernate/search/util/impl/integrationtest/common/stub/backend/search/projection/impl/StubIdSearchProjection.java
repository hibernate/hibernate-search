/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

public class StubIdSearchProjection<I> implements StubSearchProjection<I> {

	private final DocumentIdentifierValueConverter<? extends I> identifierValueConverter;

	private StubIdSearchProjection(DocumentIdentifierValueConverter<? extends I> identifierValueConverter) {
		this.identifierValueConverter = identifierValueConverter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionFromIndex;
	}

	@SuppressWarnings("unchecked")
	@Override
	public I transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		DocumentReference documentReference = (DocumentReference) extractedData;

		context.fromDocumentFieldValueConvertContext();
		return identifierValueConverter.convertToSource(
				documentReference.id(), context.fromDocumentIdentifierValueConvertContext() );
	}

	public static class Builder<I> implements IdProjectionBuilder<I> {

		private final StubSearchProjection<I> projection;

		public Builder(DocumentIdentifierValueConverter<?> identifierValueConverter, Class<I> identifierType) {
			// check expected identifier type:
			identifierValueConverter.requiresType( identifierType );
			@SuppressWarnings("uncheked") // just checked
			DocumentIdentifierValueConverter<? extends I> casted = (DocumentIdentifierValueConverter<? extends I>) identifierValueConverter;

			projection = new StubIdSearchProjection( casted );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
