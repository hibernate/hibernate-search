/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface SpatialWithinCirclePredicateBuilder extends SearchPredicateBuilder {

	void circle(GeoPoint center, double radius, DistanceUnit unit);

}
