/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractLuceneBucketAggregation<K, V> implements LuceneSearchAggregation<Map<K, V>> {

	private final Set<String> indexNames;

	AbstractLuceneBucketAggregation(AbstractBuilder<K, V> builder) {
		this.indexNames = builder.searchContext.getIndexNames();
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder<K, V> implements SearchAggregationBuilder<Map<K, V>> {

		protected final LuceneSearchContext searchContext;

		public AbstractBuilder(LuceneSearchContext searchContext) {
			this.searchContext = searchContext;
		}

		@Override
		public abstract LuceneSearchAggregation<Map<K, V>> build();
	}
}
