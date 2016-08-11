/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.apache.lucene.search.Sort;
import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortDistanceContext;
import org.hibernate.search.query.dsl.sort.SortFieldContext;
import org.hibernate.search.query.dsl.sort.SortLatLongContext;
import org.hibernate.search.query.dsl.sort.SortMissingValueContext;
import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortFieldContext extends ConnectedSortAdditionalSortFieldContext implements SortFieldContext, SortLatLongContext {

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
		return getStates().createSort();
	}

	@Override
	public SortDistanceContext fromCoordinates(Coordinates coordinates) {
		getStates().setCoordinates(coordinates);
		return new ConnectedSortDistanceContext( getQueryContext(), getStates() );
	}

	@Override
	public SortLatLongContext fromLatitude(double latitude) {
		getStates().setCurrentLatitude(latitude);
		return this;
	}

	@Override
	public SortDistanceContext andLongitude(double longitude) {
		getStates().setCurrentLongitude(longitude);
		return new ConnectedSortDistanceContext( getQueryContext(), getStates() );
	}

	@Override
	public SortMissingValueContext<SortFieldContext> onMissingValue() {
		return new ConnectedSortMissingValueContext<SortFieldContext>( getQueryContext(), getStates(), this );
	}

}
