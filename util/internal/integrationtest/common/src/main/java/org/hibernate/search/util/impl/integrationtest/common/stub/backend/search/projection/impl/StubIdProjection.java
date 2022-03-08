/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;

public class StubIdProjection<I> implements StubSearchProjection<I> {

	private final ProjectionConverter<String, ? extends I> converter;

	private StubIdProjection(ProjectionConverter<String, ? extends I> converter) {
		this.converter = converter;
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

		context.fromDocumentValueConvertContext();
		return converter.fromDocumentValue( documentReference.id(),
				context.fromDocumentValueConvertContext() );
	}

	public static class Builder<I> implements IdProjectionBuilder<I> {

		private final StubSearchProjection<I> projection;

		public Builder(ProjectionConverter<String, ? extends I> converter) {
			projection = new StubIdProjection<>( converter );
		}

		@Override
		public SearchProjection<I> build() {
			return projection;
		}
	}
}
