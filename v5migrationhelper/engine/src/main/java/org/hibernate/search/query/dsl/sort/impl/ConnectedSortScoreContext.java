/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.apache.lucene.search.Sort;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortScoreContext;

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
