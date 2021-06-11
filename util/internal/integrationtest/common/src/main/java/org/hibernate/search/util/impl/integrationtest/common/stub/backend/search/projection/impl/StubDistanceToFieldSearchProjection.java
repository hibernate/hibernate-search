/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;

public abstract class StubDistanceToFieldSearchProjection<T> implements StubSearchProjection<T> {

	private StubDistanceToFieldSearchProjection() {
	}

	public static class Factory extends AbstractStubSearchQueryElementFactory<DistanceToFieldProjectionBuilder> {
		@Override
		public DistanceToFieldProjectionBuilder create(StubSearchIndexScope scope,
				StubSearchIndexNodeContext node) {
			return new Builder();
		}
	}

	static class Builder implements DistanceToFieldProjectionBuilder {
		@Override
		public void center(GeoPoint center) {
			// No-op
		}

		@Override
		public void unit(DistanceUnit unit) {
			// No-op
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
			throw new AssertionFailure( "Distance projections are not supported in the stub backend." );
		}
	}
}
