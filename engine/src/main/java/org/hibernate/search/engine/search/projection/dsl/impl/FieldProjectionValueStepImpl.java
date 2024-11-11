/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public final class FieldProjectionValueStepImpl<T>
		extends FieldProjectionOptionsStepImpl<T, T>
		implements FieldProjectionValueStep<FieldProjectionOptionsStepImpl<T, T>, T> {

	public FieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String fieldPath,
			Class<T> clazz, ValueModel valueModel) {
		super( dslContext.scope().fieldQueryElement( fieldPath, ProjectionTypeKeys.FIELD )
				.type( clazz, valueModel ),
				ProjectionCollector.nullable() );
	}

	@Override
	public <R> FieldProjectionOptionsStep<?, R> collector(ProjectionCollector.Provider<T, R> collector) {
		return new FieldProjectionOptionsStepImpl<>( fieldProjectionBuilder, collector );
	}

	@Override
	public SearchProjection<T> toProjection() {
		return fieldProjectionBuilder.build();
	}
}
