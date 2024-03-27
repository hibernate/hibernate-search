/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

public class StubSearchSortBuilderFactory
		implements SearchSortBuilderFactory {

	@Override
	public ScoreSortBuilder score() {
		return new StubSearchSort.Builder();
	}

	@Override
	public SearchSort indexOrder() {
		return new StubSearchSort.Builder().build();
	}

	@Override
	public CompositeSortBuilder composite() {
		return new StubSearchSort.Builder();
	}
}
