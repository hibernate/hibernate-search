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
public interface RangeContext extends QueryCustomization<RangeContext> {
	/**
	 * @param fieldName field/property the term query is executed on
	 * @return a {@link RangeMatchingContext}
	 */
	RangeMatchingContext onField(String fieldName);
}
