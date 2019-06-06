/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.dsl.sort.spi.AbstractNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

class ScoreSortContextImpl<B>
		extends AbstractNonEmptySortContext<B>
		implements ScoreSortContext {

	private final ScoreSortBuilder<B> builder;

	ScoreSortContextImpl(SearchSortDslContext<?, B> dslContext) {
		super( dslContext );
		this.builder = dslContext.getFactory().score();
	}

	@Override
	public ScoreSortContext order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}
}
