/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.spatial.GeoPolygon;

public interface SpatialWithinPolygonPredicateBuilder extends SearchPredicateBuilder {

	void polygon(GeoPolygon polygon);

}
