/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.util.common.SearchException;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortMissingValueContext<T> {

	/**
	 * Put documents with missing values last in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 * @return the initial context for method chaining
	 */
	T sortLast();

	/**
	 * Put documents with missing values first in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 * @return the initial context for method chaining
	 */
	T sortFirst();

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * <p>Lucene sort API limits this feature to numeric fields. As Hibernate Search sorts are currently
	 * based on the Lucene API underneath, this is only available for numeric fields for all the indexing
	 * services, Elasticsearch included.
	 * <p>Field bridges, if any, will be ignored. Thus the actual numeric value must be provided.
	 *
	 * @param value The value to use as a replacements when the field has no value in a document.
	 * @return the initial context for method chaining
	 * @throws SearchException If the field is not numeric.
	 */
	T use(Object value);
}
