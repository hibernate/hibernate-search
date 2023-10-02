/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

public final class ScoreSortOptionsStepImpl
		extends AbstractSortThenStep
		implements ScoreSortOptionsStep<ScoreSortOptionsStepImpl> {

	private final ScoreSortBuilder builder;

	public ScoreSortOptionsStepImpl(SearchSortDslContext<?, ?> dslContext) {
		super( dslContext );
		this.builder = dslContext.scope().sortBuilders().score();
	}

	@Override
	public ScoreSortOptionsStepImpl order(SortOrder order) {
		builder.order( order );
		return this;
	}

	@Override
	protected SearchSort build() {
		return builder.build();
	}
}
