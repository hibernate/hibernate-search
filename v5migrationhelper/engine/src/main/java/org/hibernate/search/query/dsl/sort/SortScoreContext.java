/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortScoreContext extends SortAdditionalSortFieldContext, SortOrder<SortScoreContext>, SortTermination {

}
