// $Id$
package org.hibernate.search.test.query;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

/**
 * @author John Griffin
 */
public class ProjectionToDelimStringResultTransformer implements ResultTransformer {

	public Object transformTuple(Object[] tuple, String[] aliases) {
		String s = tuple[0].toString();
		for (int i = 1; i < tuple.length; i++) {
			s = s + ", " + tuple[i].toString();
		}
		return s;
	}

	public List transformList(List collection) {
		return collection;
	}
}
