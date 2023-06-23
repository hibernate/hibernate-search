/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedMoreStep;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.dsl.spi.StaticSortThenStep;

public final class SearchSortFactoryExtensionStep
		implements SearchSortFactoryExtensionIfSupportedMoreStep {

	private final SearchSortFactory parent;
	private final SearchSortDslContext<?, ?> dslContext;

	private final DslExtensionState<SortFinalStep> state = new DslExtensionState<>();

	public SearchSortFactoryExtensionStep(SearchSortFactory parent,
			SearchSortDslContext<?, ?> dslContext) {
		this.parent = parent;
		this.dslContext = dslContext;
	}

	@Override
	public <T> SearchSortFactoryExtensionIfSupportedMoreStep ifSupported(
			SearchSortFactoryExtension<T> extension,
			Function<T, ? extends SortFinalStep> sortContributor) {
		state.ifSupported( extension, extension.extendOptional( parent ), sortContributor );
		return this;
	}

	@Override
	public SortThenStep orElse(Function<SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		SortFinalStep result = state.orElse( parent, sortContributor );
		return new StaticSortThenStep( dslContext, result.toSort() );
	}

	@Override
	public SortThenStep orElseFail() {
		SortFinalStep result = state.orElseFail();
		return new StaticSortThenStep( dslContext, result.toSort() );
	}
}
