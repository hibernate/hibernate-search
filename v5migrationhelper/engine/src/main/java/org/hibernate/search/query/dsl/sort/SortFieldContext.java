/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortFieldContext extends SortAdditionalSortFieldContext, SortOrder<SortFieldContext>, SortTermination {

	/**
	 * Describe how to treat missing values when doing the sorting.
	 * @return a context to specify the behavior for missing values
	 */
	SortMissingValueContext<SortFieldContext> onMissingValue();

}
