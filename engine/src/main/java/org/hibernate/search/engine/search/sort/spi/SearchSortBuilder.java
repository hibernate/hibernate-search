/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.sort.SearchSort;

/**
 * A search sort builder, i.e. an object responsible for collecting parameters
 * and then building a search sort.
 */
public interface SearchSortBuilder {

	SearchSort build();

}
