/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortOrderTermination;
import org.hibernate.search.query.dsl.sort.SortTermination;

import org.apache.lucene.search.Sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortOrderTermination extends AbstractConnectedSortContext implements SortOrderTermination {

	public ConnectedSortOrderTermination(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortTermination asc() {
		states.setAsc();
		return this;
	}

	@Override
	public SortTermination desc() {
		states.setDesc();
		return this;
	}

	@Override
	public Sort createSort() {
		states.closeSortField();
		return states.createSort();
	}
}
