/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.sort.SearchSort;

public interface CompositeSortBuilder extends SearchSortBuilder {

	void add(SearchSort sort);

}
