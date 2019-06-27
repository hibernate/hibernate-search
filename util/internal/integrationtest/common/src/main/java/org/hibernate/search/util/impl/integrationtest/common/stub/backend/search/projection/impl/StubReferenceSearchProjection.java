/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubReferenceSearchProjection<T> implements StubSearchProjection<T> {

	@SuppressWarnings("rawtypes")
	private static final StubSearchProjection INSTANCE = new StubReferenceSearchProjection();

	@SuppressWarnings("unchecked")
	public static <T> StubReferenceSearchProjection<T> get() {
		return (StubReferenceSearchProjection<T>) INSTANCE;
	}

	private StubReferenceSearchProjection() {
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, Object projectionFromIndex,
			StubSearchProjectionContext context) {
		return projectionHitMapper.convertReference( (DocumentReference) projectionFromIndex );
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData,
			StubSearchProjectionContext context) {
		return (T) extractedData;
	}
}
