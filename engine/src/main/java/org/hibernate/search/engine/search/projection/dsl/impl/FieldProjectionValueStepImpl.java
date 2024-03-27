/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public final class FieldProjectionValueStepImpl<T>
		extends FieldProjectionOptionsStepImpl<T, T>
		implements FieldProjectionValueStep<FieldProjectionOptionsStepImpl<T, T>, T> {

	public FieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath,
			Class<T> clazz,
			ValueConvert convert) {
		super( dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.FIELD )
				.type( clazz, convert ),
				ProjectionAccumulator.single() );
	}

	@Override
	public FieldProjectionOptionsStep<?, List<T>> multi() {
		return new FieldProjectionOptionsStepImpl<>( fieldProjectionBuilder, ProjectionAccumulator.list() );
	}

	@Override
	public SearchProjection<T> toProjection() {
		return fieldProjectionBuilder.build();
	}
}
