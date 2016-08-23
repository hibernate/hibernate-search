/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.apache.lucene.search.Sort;

import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.DistanceMethod;
import org.hibernate.search.query.dsl.sort.SortDistanceContext;
import org.hibernate.search.query.dsl.sort.SortMissingValueContext;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortDistanceContext extends ConnectedSortAdditionalSortFieldContext implements SortDistanceContext {
	public ConnectedSortDistanceContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortDistanceContext asc() {
		getStates().setAsc();
		return this;
	}

	@Override
	public SortDistanceContext desc() {
		getStates().setDesc();
		return this;
	}

	@Override
	public Sort createSort() {
		return getStates().createSort();
	}

	@Override
	public SortDistanceContext in(Unit unit) {
		// nothing to do for now
		return this;
	}

	@Override
	public SortDistanceContext withComputeMethod(DistanceMethod distanceMethod) {
		// TODO not implemented for now
		return null;
	}

	@Override
	public SortMissingValueContext<SortDistanceContext> onMissingValue() {
		// TODO not implemented for now since it requires new support from the distance code
		return null;
	}
}
