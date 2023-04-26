/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class StubEntityCompositeProjection<T> implements StubSearchProjection<T> {

	private final StubSearchProjection<T> delegate;

	public StubEntityCompositeProjection(StubSearchProjection<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context) {
		return delegate.extract( projectionHitMapper, projectionFromIndex, context );
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context) {
		return delegate.transform( loadingResult, extractedData, context );
	}
}
