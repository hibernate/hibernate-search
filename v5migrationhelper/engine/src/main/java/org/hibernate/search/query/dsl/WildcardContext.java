/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface WildcardContext extends QueryCustomization<WildcardContext> {
	/**
	 * @param field field/property the term query is executed on
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onField(String field);

	/**
	 * @param fields fields/properties the term query is executed on
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onFields(String... fields);

}
