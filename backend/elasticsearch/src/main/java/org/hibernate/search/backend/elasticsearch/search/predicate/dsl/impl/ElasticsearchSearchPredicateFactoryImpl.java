/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateIndexScope;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.dsl.spi.StaticPredicateFinalStep;

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateFactoryImpl
		extends AbstractSearchPredicateFactory<
				ElasticsearchSearchPredicateFactory,
				ElasticsearchSearchPredicateIndexScope<?>>
		implements ElasticsearchSearchPredicateFactory {

	public ElasticsearchSearchPredicateFactoryImpl(
			SearchPredicateDslContext<ElasticsearchSearchPredicateIndexScope<?>> dslContext) {
		super( dslContext );
	}

	@Override
	public ElasticsearchSearchPredicateFactory withRoot(String objectFieldPath) {
		return new ElasticsearchSearchPredicateFactoryImpl( dslContext.rescope(
				dslContext.scope().withRoot( objectFieldPath ) ) );
	}

	@Override
	public PredicateFinalStep fromJson(String jsonString) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromJson( jsonString ) );
	}

	@Override
	public PredicateFinalStep fromJson(JsonObject jsonObject) {
		return new StaticPredicateFinalStep( dslContext.scope().predicateBuilders().fromJson( jsonObject ) );
	}
}
