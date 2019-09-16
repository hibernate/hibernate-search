/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;


public class FieldProjectionOptionsStepImpl<T>
		implements FieldProjectionOptionsStep<FieldProjectionOptionsStepImpl<T>, T> {

	private final FieldProjectionBuilder<T> fieldProjectionBuilder;

	FieldProjectionOptionsStepImpl(SearchProjectionBuilderFactory factory, String absoluteFieldPath, Class<T> clazz,
			ValueConvert convert) {
		this.fieldProjectionBuilder = factory.field( absoluteFieldPath, clazz, convert );
	}

	@Override
	public SearchProjection<T> toProjection() {
		return fieldProjectionBuilder.build();
	}

}
