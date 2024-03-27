/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Guillaume Smet
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface SimpleQueryStringContext extends QueryCustomization<SimpleQueryStringContext> {
	/**
	 * @param field The field name the query is executed on
	 *
	 * @return {@code SimpleQueryStringMatchingContext} to continue the query
	 */
	SimpleQueryStringMatchingContext onField(String field);

	/**
	 * @param field The first field added to the list of fields (follows the same rules described below for fields)
	 * @param fields The field names the query is executed on
	 * @return {@code SimpleQueryStringMatchingContext} to continue the query
	 */
	SimpleQueryStringMatchingContext onFields(String field, String... fields);

}
