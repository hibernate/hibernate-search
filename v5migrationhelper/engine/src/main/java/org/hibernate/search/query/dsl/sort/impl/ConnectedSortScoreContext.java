/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortScoreContext;

import org.apache.lucene.search.Sort;

/**
 * @author Yoann Rodiere
 */
public class ConnectedSortScoreContext extends ConnectedSortAdditionalSortFieldContext
		implements SortScoreContext {

	public ConnectedSortScoreContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortScoreContext asc() {
		getStates().setAsc();
		return this;
	}

	@Override
	public SortScoreContext desc() {
		getStates().setDesc();
		return this;
	}

	@Override
	public Sort createSort() {
		getStates().closeSortField();
		return getStates().createSort();
	}

}
