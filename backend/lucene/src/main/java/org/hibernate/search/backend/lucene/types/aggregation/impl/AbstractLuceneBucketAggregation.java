/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.logging.impl.Log;

import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <K> The type of keys in the returned map.
 * @param <V> The type of values in the returned map.
 */
public abstract class AbstractLuceneBucketAggregation<K, V> implements LuceneSearchAggregation<Map<K, V>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private final Set<String> indexNames;
	protected final String absoluteFieldPath;
	protected final String nestedDocumentPath;
	private final Query nestedFilter;

	AbstractLuceneBucketAggregation(AbstractBuilder<K, V> builder) {
		this.indexNames = builder.searchContext.getIndexNames();
		this.absoluteFieldPath = builder.absoluteFieldPath;
		this.nestedDocumentPath = builder.nestedDocumentPath;
		this.nestedFilter = builder.getLuceneFilter();
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	public String getAbsoluteFieldPath() {
		return absoluteFieldPath;
	}

	public Query getNestedFilter() {
		return nestedFilter;
	}

	public abstract static class AbstractBuilder<K, V> implements SearchAggregationBuilder<Map<K, V>> {

		protected final LuceneSearchContext searchContext;
		private final String nestedDocumentPath;
		protected final String absoluteFieldPath;
		protected SearchPredicate filter;

		public AbstractBuilder(LuceneSearchContext searchContext, String absoluteFieldPath, String nestedDocumentPath) {
			this.searchContext = searchContext;
			this.absoluteFieldPath = absoluteFieldPath;
			this.nestedDocumentPath = nestedDocumentPath;
		}

		@Override
		public void filter(SearchPredicate filter) {
			this.filter = filter;
		}

		@Override
		public abstract LuceneSearchAggregation<Map<K, V>> build();

		protected Query getLuceneFilter() {
			if ( filter == null ) {
				return null;
			}

			Query luceneFilter = null;
			if ( filter instanceof LuceneSearchPredicateBuilder ) {
				LuceneSearchPredicateContext filterContext = new LuceneSearchPredicateContext( nestedDocumentPath );
				luceneFilter = ((LuceneSearchPredicateBuilder) filter).build( filterContext );
			}
			else {
				throw log.unableToCreateNestedAggregationFilter( nestedDocumentPath );
			}

			return luceneFilter;
		}

	}
}
