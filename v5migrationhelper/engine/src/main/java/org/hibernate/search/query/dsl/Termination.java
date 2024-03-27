/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface Termination<T> {
	/**
	 * Return the lucene query representing the operation
	 * @return the lucene query representing the operation
	 */
	Query createQuery();
}
