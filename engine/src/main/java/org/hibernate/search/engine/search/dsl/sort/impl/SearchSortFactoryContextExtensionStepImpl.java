/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtensionStep;
import org.hibernate.search.engine.search.dsl.sort.SortFinalStep;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticSortThenStep;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;


final class SearchSortFactoryContextExtensionStepImpl<B> implements SearchSortFactoryContextExtensionStep {

	private final SearchSortFactoryContext parent;
	private final SearchSortDslContext<?, B> dslContext;

	private final DslExtensionState<SortFinalStep> state = new DslExtensionState<>();

	SearchSortFactoryContextExtensionStepImpl(SearchSortFactoryContext parent,
			SearchSortDslContext<?, B> dslContext) {
		this.parent = parent;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortFactoryContextExtensionStep ifSupported(
			SearchSortFactoryContextExtension<T> extension,
			Function<T, ? extends SortFinalStep> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent, dslContext ), sortContributor );
		return this;
	}

	@Override
	public SortThenStep orElse(Function<SearchSortFactoryContext, ? extends SortFinalStep> sortContributor) {
		SortFinalStep result = state.orElse( parent, sortContributor );
		return new StaticSortThenStep<>( dslContext, dslContext.getFactory().toImplementation( result.toSort() ) );
	}

	@Override
	public SortThenStep orElseFail() {
		SortFinalStep result = state.orElseFail();
		return new StaticSortThenStep<>( dslContext, dslContext.getFactory().toImplementation( result.toSort() ) );
	}
}
