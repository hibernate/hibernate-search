/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.dsl.spi.AbstractSortThenStep;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.WithParametersSortBuilder;

public class WithParametersSortFinalStep extends AbstractSortThenStep {

	private final WithParametersSortBuilder builder;

	public WithParametersSortFinalStep(SearchSortDslContext<?, ?> dslContext,
			Function<? super NamedValues, ? extends SortFinalStep> sortCreator) {
		super( dslContext );
		builder = dslContext.scope().sortBuilders().withParameters();
		builder.creator( sortCreator );
	}


	@Override
	protected SearchSort build() {
		return builder.build();
	}

}
