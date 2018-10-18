/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.DistanceFieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.projection.spi.DistanceFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.Contracts;


public class DistanceFieldProjectionContextImpl implements DistanceFieldProjectionContext {

	private final DistanceFieldSearchProjectionBuilder distanceFieldProjectionBuilder;

	DistanceFieldProjectionContextImpl(SearchProjectionFactory factory, String absoluteFieldPath, GeoPoint center) {
		this.distanceFieldProjectionBuilder = factory.distance( absoluteFieldPath, center );
	}

	@Override
	public SearchProjectionTerminalContext<Double> unit(DistanceUnit unit) {
		Contracts.assertNotNull( unit, "unit" );

		distanceFieldProjectionBuilder.unit( unit );
		return this;
	}

	@Override
	public SearchProjection<Double> toProjection() {
		return distanceFieldProjectionBuilder.build();
	}

}
