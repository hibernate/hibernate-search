/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.List;

/**
 * Keep count of the number of times a method get called.
 *
 * @author Davide D'Alto
 */
public class CounterCallsProjectionToMapResultTransformer extends ProjectionToMapResultTransformer {

	private int transformTupleCounter = 0;
	private int transformListCounter = 0;

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		transformTupleCounter++;
		return super.transformTuple( tuple, aliases );
	}

	@Override
	public List transformList(List collection) {
		transformListCounter++;
		return super.transformList( collection );
	}

	public int getTransformTupleCounter() {
		return transformTupleCounter;
	}

	public int getTransformListCounter() {
		return transformListCounter;
	}
}
