/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;

public class ElasticsearchSearchQueryContextImpl<Q> extends AbstractDelegatingSearchQueryContext<
						ElasticsearchSearchQueryContext<Q>,
						Q
						>
		implements ElasticsearchSearchQueryResultContext<Q>, ElasticsearchSearchQueryContext<Q> {

	// FIXME use the builder to make toQuery return an Elasticsearch-specific query type
	private final ElasticsearchSearchQueryBuilder<?> builder;

	public ElasticsearchSearchQueryContextImpl(SearchQueryContextImplementor<?, Q> original,
			ElasticsearchSearchQueryBuilder<?> builder) {
		super( original );
		this.builder = builder;
	}

	@Override
	protected ElasticsearchSearchQueryContextImpl<Q> thisAsS() {
		return this;
	}

}
