/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

/**
 * @author John Griffin
 */
public class ProjectionToDelimStringResultTransformer implements ResultTransformer {

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		String s = tuple[0].toString();
		for ( int i = 1; i < tuple.length; i++ ) {
			s = s + ", " + tuple[i].toString();
		}
		return s;
	}

	@Override
	public List transformList(List collection) {
		return collection;
	}
}
