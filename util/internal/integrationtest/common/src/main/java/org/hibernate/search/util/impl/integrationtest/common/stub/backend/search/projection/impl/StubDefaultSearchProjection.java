/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class StubDefaultSearchProjection<T> implements StubSearchProjection<T> {

	@SuppressWarnings("rawtypes")
	private static final StubSearchProjection INSTANCE = new StubDefaultSearchProjection();

	@SuppressWarnings("unchecked")
	static <T> StubDefaultSearchProjection<T> get() {
		return (StubDefaultSearchProjection<T>) INSTANCE;
	}

	private StubDefaultSearchProjection() {
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			FromDocumentFieldValueConvertContext context) {
		return projectionFromIndex;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		return (T) extractedData;
	}
}
