/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ListProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.SingleValuedProjectionAccumulator;
import org.hibernate.search.engine.spatial.GeoPoint;

public class DistanceToFieldProjectionValueStepImpl
		extends DistanceToFieldProjectionOptionsStepImpl<Double>
		implements DistanceToFieldProjectionValueStep<DistanceToFieldProjectionOptionsStepImpl<Double>, Double> {

	DistanceToFieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String absoluteFieldPath,
			GeoPoint center) {
		super( dslContext.builderFactory().distance( absoluteFieldPath, center ),
				SingleValuedProjectionAccumulator.provider() );
	}

	@Override
	public DistanceToFieldProjectionOptionsStep<?, List<Double>> multi() {
		return new DistanceToFieldProjectionOptionsStepImpl<>( distanceFieldProjectionBuilder,
				ListProjectionAccumulator.provider() );
	}

}
