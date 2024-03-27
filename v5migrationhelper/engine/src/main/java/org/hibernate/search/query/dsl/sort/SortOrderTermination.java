/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortOrderTermination extends SortTermination, SortOrder<SortTermination> {
}
