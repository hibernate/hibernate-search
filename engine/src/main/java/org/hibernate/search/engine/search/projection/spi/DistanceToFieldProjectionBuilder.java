/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface DistanceToFieldProjectionBuilder extends SearchProjectionBuilder<Double> {

	void center(GeoPoint center);

	void unit(DistanceUnit unit);

	@Override
	default SearchProjection<Double> build() {
		return build( ProjectionCollector.nullable() );
	}

	<P> SearchProjection<P> build(ProjectionCollector.Provider<Double, P> collectorProvider);

}
