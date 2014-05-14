/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.transform.ResultTransformer;

/**
 * @author John Griffin
 */
public class ProjectionToMapResultTransformer implements ResultTransformer {

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		Map result = new HashMap( tuple.length );
		for ( int i = 0; i < tuple.length; i++ ) {
			String key = aliases[i];
			if ( key != null ) {
				result.put( key, tuple[i] );
			}
		}
		return result;
	}

	@Override
	public List transformList(List collection) {
		return collection;
	}
}
