/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public abstract class StubDistanceToFieldProjection<T> extends StubSearchProjection<T> {

	private StubDistanceToFieldProjection() {
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
