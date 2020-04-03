/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Map;
import java.util.Set;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.common.MultiValue;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractLuceneBucketAggregation<K, V> implements LuceneSearchAggregation<Map<K, V>> {

	private final Set<String> indexNames;
	private final MultiValueMode multiValueMode;

	AbstractLuceneBucketAggregation(AbstractBuilder<K, V> builder) {
		this.indexNames = builder.searchContext.getIndexNames();
		this.multiValueMode = builder.getMultiValueMode( builder.mode );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	public MultiValueMode getMultiValueMode() {
		return multiValueMode;
	}

	public abstract static class AbstractBuilder<K, V> implements SearchAggregationBuilder<Map<K, V>> {

		protected final LuceneSearchContext searchContext;
		protected MultiValue mode;

		public AbstractBuilder(LuceneSearchContext searchContext) {
			this.searchContext = searchContext;
		}

		@Override
		public void mode(MultiValue mode) {
			this.mode = mode;
		}

		@Override
		public abstract LuceneSearchAggregation<Map<K, V>> build();

		protected MultiValueMode getMultiValueMode(MultiValue multi) {
			MultiValueMode valueMode = MultiValueMode.NONE;
			if ( multi != null ) {
				switch ( multi ) {
					case MIN:
						valueMode = MultiValueMode.MIN;
						break;
					case MAX:
						valueMode = MultiValueMode.MAX;
						break;
					case AVG:
						valueMode = MultiValueMode.AVG;
						break;
					case SUM:
						valueMode = MultiValueMode.SUM;
						break;
					case MEDIAN:
						valueMode = MultiValueMode.MEDIAN;
						break;
				}
			}
			return valueMode;
		}
	}

}
