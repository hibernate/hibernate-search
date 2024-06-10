/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortFieldContext;
import org.hibernate.search.query.dsl.sort.SortMissingValueContext;

import org.apache.lucene.search.Sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortFieldContext extends ConnectedSortAdditionalSortFieldContext
		implements SortFieldContext, SortMissingValueContext<SortFieldContext> {

	public ConnectedSortFieldContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortFieldContext asc() {
		getStates().setAsc();
		return this;
	}

	@Override
	public SortFieldContext desc() {
		getStates().setDesc();
		return this;
	}

	@Override
	public Sort createSort() {
		getStates().closeSortField();
		return getStates().createSort();
	}

	@Override
	public SortMissingValueContext<SortFieldContext> onMissingValue() {
		return this;
	}

	@Override
	public SortFieldContext sortLast() {
		getStates().setCurrentMissingValueLast();
		return this;
	}

	@Override
	public SortFieldContext sortFirst() {
		getStates().setCurrentMissingValueFirst();
		return this;
	}

	@Override
	public SortFieldContext use(Object value) {
		getStates().setCurrentMissingValue( value );
		return this;
	}

}
