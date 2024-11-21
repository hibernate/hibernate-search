/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.sort.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractLuceneSort implements LuceneSearchSort {

	private final Set<String> indexNames;

	protected AbstractLuceneSort(AbstractBuilder builder) {
		this( builder.scope );
	}

	protected AbstractLuceneSort(LuceneSearchIndexScope<?> scope) {
		indexNames = scope.hibernateSearchIndexNames();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder implements SearchSortBuilder {
		protected final LuceneSearchIndexScope<?> scope;

		protected AbstractBuilder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}
	}
}
