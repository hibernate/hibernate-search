/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort.impl;

import org.hibernate.search.query.dsl.impl.QueryBuildingContext;
import org.hibernate.search.query.dsl.sort.SortMissingValueContext;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class ConnectedSortMissingValueContext<T> extends ConnectedSortAdditionalSortFieldContext implements SortMissingValueContext<T>, SortMissingValueContext.ValueTreatmentContext<T> {
	private T returnValue;

	public ConnectedSortMissingValueContext(QueryBuildingContext queryContext, SortFieldStates states, T returnValue) {
		super( queryContext, states );
		this.returnValue = returnValue;
	}

	@Override
	public T sortLast() {
		getStates().setCurrentMissingValueLast();
		return returnValue;
	}

	@Override
	public T sortFirst() {
		getStates().setCurrentMissingValueFirst();
		return returnValue;
	}

	@Override
	public ValueTreatmentContext<T> use(Object value) {
		getStates().setCurrentMissingValue( value );
		return this;
	}

	@Override
	public T ignoreFieldBridge() {
		getStates().setCurrentIgnoreFieldBridge();
		return returnValue;
	}
}
