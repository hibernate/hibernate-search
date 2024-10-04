/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.SearchProjection;

public interface FieldProjectionBuilder<T> extends SearchProjectionBuilder<T> {

	interface TypeSelector {
		<T> FieldProjectionBuilder<T> type(Class<T> expectedType, ValueModel valueModel);
	}

	@Override
	default SearchProjection<T> build() {
		return build( ProjectionAccumulator.nullable() );
	}

	<P> SearchProjection<P> build(ProjectionAccumulator.Provider<T, P> accumulatorProvider);

}
