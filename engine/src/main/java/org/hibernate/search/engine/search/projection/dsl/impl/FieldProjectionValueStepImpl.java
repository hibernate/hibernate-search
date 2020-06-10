/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.ListProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.SingleValuedProjectionAccumulator;


public class FieldProjectionValueStepImpl<T>
		extends FieldProjectionOptionsStepImpl<T, T>
		implements FieldProjectionValueStep<FieldProjectionOptionsStepImpl<T, T>, T> {

	FieldProjectionValueStepImpl(SearchProjectionDslContext<?> dslContext, String absoluteFieldPath, Class<T> clazz,
			ValueConvert convert) {
		super( dslContext.builderFactory().field( absoluteFieldPath, clazz, convert ),
				SingleValuedProjectionAccumulator.provider() );
	}

	@Override
	public FieldProjectionOptionsStep<?, List<T>> multi() {
		return new FieldProjectionOptionsStepImpl<>( fieldProjectionBuilder, ListProjectionAccumulator.provider() );
	}

	@Override
	public SearchProjection<T> toProjection() {
		return fieldProjectionBuilder.build();
	}
}
