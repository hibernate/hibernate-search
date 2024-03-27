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
public interface AllContext extends QueryCustomization<AllContext>, Termination<AllContext> {
	/**
	 * Exclude the documents matching these queries
	 * @param queriesMatchingExcludedDocuments the queries to use for excluding documents
	 * @return {@code this} for method chaining
	 */
	AllContext except(Query... queriesMatchingExcludedDocuments);
}
