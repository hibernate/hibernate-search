/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.HashMap;

import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.search.common.spi.MapNamedValues;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Contracts;

@Incubating
public class QueryParameters extends MapNamedValues {

	public QueryParameters() {
		super( new HashMap<>(),
				QueryLog.INSTANCE::cannotFindQueryParameter,
				QueryLog.INSTANCE::unexpectedQueryParameterType );
	}

	public void add(String parameter, Object value) {
		Contracts.assertNotNullNorEmpty( parameter, "parameter" );
		values.put( parameter, value );
	}
}
