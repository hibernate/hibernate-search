/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.queryparser.simple.SimpleQueryParser;

/**
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface SimpleQueryStringDefinitionTermination {

	/**
	 * Simple query string passed to the {@link SimpleQueryParser}.
	 * @param simpleQueryString The query string
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringTermination matching(String simpleQueryString);

}
