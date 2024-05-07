/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

public final class ScoreSortOptionsStepImpl<E>
		extends AbstractSortThenStep<E>
		implements ScoreSortOptionsStep<E, ScoreSortOptionsStepImpl<E>> {

	private final ScoreSortBuilder builder;

	public ScoreSortOptionsStepImpl(SearchSortDslContext<E, ?, ?> dslContext) {
		super( dslContext );
		this.builder = dslContext.scope().sortBuilders().score();
	}

	@Override
	public ScoreSortOptionsStepImpl<E> order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}
}
