/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface DistanceToFieldProjectionBuilder extends SearchProjectionBuilder<Double> {

	void center(GeoPoint center);

	void unit(DistanceUnit unit);

	@Override
	default SearchProjection<Double> build() {
		return build( ProjectionAccumulator.single() );
	}

	<P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider);

}
