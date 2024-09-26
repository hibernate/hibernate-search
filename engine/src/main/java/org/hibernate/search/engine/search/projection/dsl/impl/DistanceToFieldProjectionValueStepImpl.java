/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.spatial.GeoPoint;

public final class DistanceToFieldProjectionValueStepImpl
		extends DistanceToFieldProjectionOptionsStepImpl<Double>
		implements DistanceToFieldProjectionValueStep<DistanceToFieldProjectionOptionsStepImpl<Double>, Double> {

	public DistanceToFieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath,
			GeoPoint center) {
		super( dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.DISTANCE ), ProjectionAccumulator.single() );
		distanceFieldProjectionBuilder.center( center );
	}

	@Override
	public <C> DistanceToFieldProjectionOptionsStep<?, C> multi(
			MultiProjectionTypeReference<C, Double> collectionTypeReference) {
		return new DistanceToFieldProjectionOptionsStepImpl<>( distanceFieldProjectionBuilder,
				ProjectionAccumulator.multi( collectionTypeReference ) );
	}

}
