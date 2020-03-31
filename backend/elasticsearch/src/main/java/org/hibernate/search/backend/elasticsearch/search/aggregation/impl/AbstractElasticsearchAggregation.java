/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;
import org.hibernate.search.engine.search.common.MultiValue;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

public abstract class AbstractElasticsearchAggregation<A> implements ElasticsearchSearchAggregation<A> {

	private final Set<String> indexNames;

	AbstractElasticsearchAggregation(AbstractBuilder<A> builder) {
		this.indexNames = builder.searchContext.getHibernateSearchIndexNames();
	}

	@Override
	public final Set<String> getIndexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder<A> implements SearchAggregationBuilder<A> {

		protected final ElasticsearchSearchContext searchContext;
		protected MultiValue mode;
		protected SearchPredicate filter;

		public AbstractBuilder(ElasticsearchSearchContext searchContext) {
			this.searchContext = searchContext;
		}

		@Override
		public void mode(MultiValue multi) {
			this.mode = multi;
		}

		@Override
		public void filter(SearchPredicate filter) {
			this.filter = filter;
		}

		@Override
		public abstract ElasticsearchSearchAggregation<A> build();
	}
}
