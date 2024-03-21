/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.util.common.impl.Contracts;

public class DistanceToFieldProjectionOptionsStepImpl<P>
		implements DistanceToFieldProjectionOptionsStep<DistanceToFieldProjectionOptionsStepImpl<P>, P> {

	protected final DistanceToFieldProjectionBuilder distanceFieldProjectionBuilder;
	private final ProjectionAccumulator.Provider<Double, P> accumulatorProvider;

	DistanceToFieldProjectionOptionsStepImpl(DistanceToFieldProjectionBuilder distanceFieldProjectionBuilder,
			ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
		this.distanceFieldProjectionBuilder = distanceFieldProjectionBuilder;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public DistanceToFieldProjectionOptionsStepImpl<P> unit(DistanceUnit unit) {
		Contracts.assertNotNull( unit, "unit" );

		distanceFieldProjectionBuilder.unit( unit );
		return this;
	}

	@Override
	public DistanceToFieldProjectionOptionsStepImpl<P> unitParam(String parameterName) {
		Contracts.assertNotNullNorEmpty( parameterName, "parameterName" );

		distanceFieldProjectionBuilder.unitParam( parameterName );
		return this;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return distanceFieldProjectionBuilder.build( accumulatorProvider );
	}

}
