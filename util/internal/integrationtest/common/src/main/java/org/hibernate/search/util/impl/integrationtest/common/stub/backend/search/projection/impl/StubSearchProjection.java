/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.Iterator;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.AssertionFailure;

public interface StubSearchProjection<P> extends SearchProjection<P> {

	Object extract(ProjectionHitMapper<?> projectionHitMapper, Iterator<?> projectionFromIndex,
			StubSearchProjectionContext context);

	P transform(LoadingResult<?> loadingResult, Object extractedData, StubSearchProjectionContext context);

	static <U> StubSearchProjection<U> from(SearchProjection<U> projection) {
		if ( !( projection instanceof StubSearchProjection ) ) {
			throw new AssertionFailure( "Projection " + projection + " must be a StubSearchProjection" );
		}
		return (StubSearchProjection<U>) projection;
	}
}
