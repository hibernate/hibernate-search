/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.DelegatingSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.spi.StaticPredicateFinalStep;

import com.google.gson.JsonObject;


public class ElasticsearchSearchPredicateFactoryImpl
		extends DelegatingSearchPredicateFactory
		implements ElasticsearchSearchPredicateFactory {

	private final SearchPredicateDslContext<ElasticsearchSearchPredicateBuilderFactory> dslContext;

	public ElasticsearchSearchPredicateFactoryImpl(SearchPredicateFactory delegate,
			SearchPredicateDslContext<ElasticsearchSearchPredicateBuilderFactory> dslContext) {
		super( delegate );
		this.dslContext = dslContext;
	}

	@Override
	public PredicateFinalStep fromJson(String jsonString) {
		return new StaticPredicateFinalStep( dslContext.builderFactory().fromJson( jsonString ) );
	}

	@Override
	public PredicateFinalStep fromJson(JsonObject jsonObject) {
		return new StaticPredicateFinalStep( dslContext.builderFactory().fromJson( jsonObject ) );
	}
}
