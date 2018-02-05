/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortMissingValueContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

class FieldSortContextImpl<N, C>
		implements FieldSortContext<N>, FieldSortMissingValueContext<FieldSortContext<N>>, SearchSortContributor<C> {

	private final SearchSortContainerContext<N> containerContext;
	private final Supplier<N> nextContextProvider;
	private final FieldSortBuilder<C> builder;

	FieldSortContextImpl(SearchSortContainerContext<N> containerContext,
			SearchSortFactory<C> factory, Supplier<N> nextContextProvider,
			String absoluteFieldPath) {
		this.containerContext = containerContext;
		this.nextContextProvider = nextContextProvider;
		this.builder = factory.field( absoluteFieldPath );
	}

	@Override
	public FieldSortContext<N> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public FieldSortMissingValueContext<FieldSortContext<N>> onMissingValue() {
		return this;
	}

	@Override
	public FieldSortContext<N> sortFirst() {
		builder.missingFirst();
		return this;
	}

	@Override
	public FieldSortContext<N> sortLast() {
		builder.missingLast();
		return this;
	}

	@Override
	public FieldSortContext<N> use(Object value) {
		builder.missingAs( value );
		return this;
	}

	@Override
	public SearchSortContainerContext<N> then() {
		return containerContext;
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	@Override
	public void contribute(C collector) {
		builder.contribute( collector );
	}
}
