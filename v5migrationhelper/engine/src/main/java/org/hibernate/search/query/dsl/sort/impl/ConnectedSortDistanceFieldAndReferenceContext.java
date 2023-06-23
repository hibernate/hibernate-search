/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortDistanceFieldAndReferenceContext;

import org.apache.lucene.search.Sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortDistanceFieldAndReferenceContext extends ConnectedSortAdditionalSortFieldContext
		implements SortDistanceFieldAndReferenceContext {
	public ConnectedSortDistanceFieldAndReferenceContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortDistanceFieldAndReferenceContext asc() {
		getStates().setAsc();
		return this;
	}

	@Override
	public SortDistanceFieldAndReferenceContext desc() {
		getStates().setDesc();
		return this;
	}

	@Override
	public Sort createSort() {
		getStates().closeSortField();
		return getStates().createSort();
	}
}
