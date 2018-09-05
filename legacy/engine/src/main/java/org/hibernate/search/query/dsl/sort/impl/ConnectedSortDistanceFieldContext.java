/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortDistanceFieldAndReferenceContext;
import org.hibernate.search.query.dsl.sort.SortDistanceFieldContext;
import org.hibernate.search.query.dsl.sort.SortLatLongContext;
import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public class ConnectedSortDistanceFieldContext extends AbstractConnectedSortContext
		implements SortDistanceFieldContext, SortLatLongContext {

	public ConnectedSortDistanceFieldContext(QueryBuildingContext queryContext, SortFieldStates states) {
		super( queryContext, states );
	}

	@Override
	public SortDistanceFieldAndReferenceContext fromCoordinates(Coordinates coordinates) {
		getStates().setCoordinates( coordinates );
		return new ConnectedSortDistanceFieldAndReferenceContext( getQueryContext(), getStates() );
	}

	@Override
	public SortLatLongContext fromLatitude(double latitude) {
		getStates().setCurrentLatitude( latitude );
		return this;
	}

	@Override
	public SortDistanceFieldAndReferenceContext andLongitude(double longitude) {
		getStates().setCurrentLongitude( longitude );
		return new ConnectedSortDistanceFieldAndReferenceContext( getQueryContext(), getStates() );
	}

}
