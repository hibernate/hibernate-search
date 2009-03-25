// $Id$
package org.hibernate.search.test.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.transform.ResultTransformer;

/**
 * @author John Griffin
 */
public class ProjectionToMapResultTransformer implements ResultTransformer {

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Map result = new HashMap( tuple.length );
		for (int i = 0; i < tuple.length; i++) {
			String key = aliases[i];
			if ( key != null ) {
				result.put( key, tuple[i] );
			}
		}
		return result;
	}

	public List transformList(List collection) {
		return collection;
	}
}
