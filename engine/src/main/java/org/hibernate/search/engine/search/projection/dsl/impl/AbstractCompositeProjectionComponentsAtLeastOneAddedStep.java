/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponentsAtLeast1AddedStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;

abstract class AbstractCompositeProjectionComponentsAtLeastOneAddedStep
		extends AbstractCompositeProjectionComponentsAddStep
		implements CompositeProjectionComponentsAtLeast1AddedStep {

	public AbstractCompositeProjectionComponentsAtLeastOneAddedStep(SearchProjectionDslContext<?> dslContext) {
		super( dslContext );
	}

	@Override
	public <T> CompositeProjectionComponentsAtLeast1AddedStep add(SearchProjection<T> projection) {
		return new CompositeProjectionComponentUnknownNumberAddedStep(
				dslContext, toProjectionArray( projection ) );
	}

	@Override
	public final CompositeProjectionOptionsStep<?, List<?>> asList() {
		return transformList( Function.identity() );
	}

	@Override
	public final <V> CompositeProjectionOptionsStep<?, V> transformList(Function<List<?>, V> transformer) {
		return new CompositeProjectionFinalStep<>( dslContext, transformer, toProjectionArray() );
	}

	abstract SearchProjection<?>[] toProjectionArray();

}
