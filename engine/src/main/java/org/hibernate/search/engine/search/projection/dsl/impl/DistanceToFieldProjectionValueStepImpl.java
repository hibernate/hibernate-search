/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionFromStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.spatial.GeoPoint;

public final class DistanceToFieldProjectionValueStepImpl
		extends DistanceToFieldProjectionOptionsStepImpl<Double>
		implements DistanceToFieldProjectionValueStep<DistanceToFieldProjectionOptionsStepImpl<Double>, Double>,
		DistanceToFieldProjectionFromStep<DistanceToFieldProjectionValueStepImpl, Double> {

	public DistanceToFieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath) {
		super( dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.DISTANCE ),
				ProjectionAccumulator.single() );
	}

	@Override
	public DistanceToFieldProjectionOptionsStep<?, List<Double>> multi() {
		return new DistanceToFieldProjectionOptionsStepImpl<>( distanceFieldProjectionBuilder,
				ProjectionAccumulator.list() );
	}


	@Override
	public DistanceToFieldProjectionValueStep<?, Double> from(GeoPoint center) {
		distanceFieldProjectionBuilder.center( center );
		return this;
	}

	@Override
	public DistanceToFieldProjectionValueStep<?, Double> fromParam(String parameterName) {
		distanceFieldProjectionBuilder.centerParam( parameterName );
		return this;
	}
}
