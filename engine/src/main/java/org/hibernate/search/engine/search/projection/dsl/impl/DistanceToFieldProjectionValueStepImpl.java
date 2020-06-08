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
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;


public class DistanceToFieldProjectionValueStepImpl
		extends DistanceToFieldProjectionOptionsStepImpl
		implements DistanceToFieldProjectionValueStep<DistanceToFieldProjectionOptionsStepImpl, Double> {

	DistanceToFieldProjectionValueStepImpl(SearchProjectionBuilderFactory factory, String absoluteFieldPath,
			GeoPoint center) {
		super( factory.distance( absoluteFieldPath, center ) );
	}

	@Override
	public DistanceToFieldProjectionOptionsStep<?, List<Double>> multi() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

}
