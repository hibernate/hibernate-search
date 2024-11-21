/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractLuceneBucketAggregation<K, V> extends AbstractLuceneNestableAggregation<Map<K, V>>
		implements LuceneSearchAggregation<Map<K, V>> {

	private final Set<String> indexNames;
	protected final String absoluteFieldPath;

	AbstractLuceneBucketAggregation(AbstractBuilder<K, V> builder) {
		super( builder );
		this.indexNames = builder.scope.hibernateSearchIndexNames();
		this.absoluteFieldPath = builder.field.absolutePath();
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder<K, V> extends AbstractLuceneNestableAggregation.AbstractBuilder<Map<K, V>>
			implements SearchAggregationBuilder<Map<K, V>> {

		public AbstractBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field) {
			super( scope, field );
		}

		@Override
		public abstract LuceneSearchAggregation<Map<K, V>> build();
	}
}
