/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilder;

public abstract class AbstractElasticsearchSort implements ElasticsearchSearchSort {

	protected final Set<String> indexNames;

	protected AbstractElasticsearchSort(AbstractBuilder builder) {
		this( builder.scope );
	}

	protected AbstractElasticsearchSort(ElasticsearchSearchIndexScope<?> scope) {
		indexNames = scope.hibernateSearchIndexNames();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder implements SearchSortBuilder {

		protected final ElasticsearchSearchIndexScope<?> scope;

		protected AbstractBuilder(ElasticsearchSearchIndexScope<?> scope) {
			this.scope = scope;
		}
	}
}
