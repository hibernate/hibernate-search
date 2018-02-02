/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;


public class ElasticsearchSearchPredicateContainerContextImpl<N>
		extends DelegatingSearchPredicateContainerContextImpl<N>
		implements ElasticsearchSearchPredicateContainerContext<N> {

	private final ElasticsearchSearchPredicateFactory factory;

	private final SearchPredicateDslContext<N, ElasticsearchSearchPredicateCollector> dslContext;

	public ElasticsearchSearchPredicateContainerContextImpl(SearchPredicateContainerContext<N> delegate,
			ElasticsearchSearchPredicateFactory factory,
			SearchPredicateDslContext<N, ElasticsearchSearchPredicateCollector> dslContext) {
		super( delegate );
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public N fromJsonString(String jsonString) {
		dslContext.addContributor( factory.fromJsonString( jsonString ) );
		return dslContext.getNextContext();
	}
}
