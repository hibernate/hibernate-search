/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

public abstract class AbstractElasticsearchAggregation<A> implements ElasticsearchSearchAggregation<A> {

	private final Set<String> indexNames;

	AbstractElasticsearchAggregation(AbstractBuilder<A> builder) {
		this.indexNames = builder.scope.hibernateSearchIndexNames();
	}

	@Override
	public final Set<String> indexNames() {
		return indexNames;
	}

	public abstract static class AbstractBuilder<A> implements SearchAggregationBuilder<A> {

		protected final ElasticsearchSearchIndexScope<?> scope;

		public AbstractBuilder(ElasticsearchSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		@Override
		public abstract ElasticsearchSearchAggregation<A> build();
	}
}
