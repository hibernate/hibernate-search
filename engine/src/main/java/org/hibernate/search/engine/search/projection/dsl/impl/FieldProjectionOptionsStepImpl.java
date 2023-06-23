/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

public class FieldProjectionOptionsStepImpl<T, P>
		implements FieldProjectionOptionsStep<FieldProjectionOptionsStepImpl<T, P>, P> {

	protected final FieldProjectionBuilder<T> fieldProjectionBuilder;
	private final ProjectionAccumulator.Provider<T, P> accumulatorProvider;

	FieldProjectionOptionsStepImpl(FieldProjectionBuilder<T> fieldProjectionBuilder,
			ProjectionAccumulator.Provider<T, P> accumulatorProvider) {
		this.fieldProjectionBuilder = fieldProjectionBuilder;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public SearchProjection<P> toProjection() {
		return fieldProjectionBuilder.build( accumulatorProvider );
	}

}
