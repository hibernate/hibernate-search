/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.converter.impl.StubFieldConverter;

class StubFieldSearchProjection<T> implements StubSearchProjection<T> {
	private final Class<T> expectedType;
	private final StubFieldConverter<?> converter;

	StubFieldSearchProjection(Class<T> expectedType, StubFieldConverter<?> converter) {
		this.expectedType = expectedType;
		this.converter = converter;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return converter.convertIndexToProjection(
				projectionFromIndex, context.getFromDocumentFieldValueConvertContext()
		);
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return expectedType.cast( extractedData );
	}
}
