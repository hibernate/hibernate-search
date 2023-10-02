/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
