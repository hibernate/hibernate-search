/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

class ScoreSortContextImpl<B>
		extends NonEmptySortContextImpl
		implements ScoreSortContext, SearchSortContributor<B> {

	private final ScoreSortBuilder<B> builder;

	ScoreSortContextImpl(SearchSortContainerContext containerContext,
			SearchSortFactory<?, B> factory, SearchSortDslContext<?> dslContext) {
		super( containerContext, dslContext );
		this.builder = factory.score();
	}

	@Override
	public ScoreSortContext order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	public void contribute(Consumer<? super B> collector) {
		collector.accept( builder.toImplementation() );
	}
}
