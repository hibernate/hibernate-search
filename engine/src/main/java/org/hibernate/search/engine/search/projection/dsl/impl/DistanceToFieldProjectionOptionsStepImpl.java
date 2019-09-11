/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.Contracts;


public class DistanceToFieldProjectionOptionsStepImpl implements DistanceToFieldProjectionOptionsStep {

	private final DistanceToFieldProjectionBuilder distanceFieldProjectionBuilder;

	DistanceToFieldProjectionOptionsStepImpl(SearchProjectionBuilderFactory factory, String absoluteFieldPath, GeoPoint center) {
		this.distanceFieldProjectionBuilder = factory.distance( absoluteFieldPath, center );
	}

	@Override
	public ProjectionFinalStep<Double> unit(DistanceUnit unit) {
		Contracts.assertNotNull( unit, "unit" );

		distanceFieldProjectionBuilder.unit( unit );
		return this;
	}

	@Override
	public SearchProjection<Double> toProjection() {
		return distanceFieldProjectionBuilder.build();
	}

}
