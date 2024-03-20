/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface SpatialWithinCirclePredicateBuilder extends SearchPredicateBuilder {

	void circle(GeoPoint center, double radius, DistanceUnit unit);

	void param(String center, String radius, String unit);
}
