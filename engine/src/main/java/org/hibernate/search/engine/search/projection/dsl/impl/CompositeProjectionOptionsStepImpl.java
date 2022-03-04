/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;


public class CompositeProjectionOptionsStepImpl<T>
		implements CompositeProjectionOptionsStep<CompositeProjectionOptionsStepImpl<T>, T> {

	private final CompositeProjectionBuilder<T> compositeProjectionBuilder;

	public CompositeProjectionOptionsStepImpl(CompositeProjectionBuilder<T> compositeProjectionBuilder) {
		this.compositeProjectionBuilder = compositeProjectionBuilder;
	}

	@Override
	public SearchProjection<T> toProjection() {
		return compositeProjectionBuilder.build();
	}
}
