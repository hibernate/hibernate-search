/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateFactory;


public class ElasticsearchSearchPredicateFactoryImpl
		extends DelegatingSearchPredicateFactory
		implements ElasticsearchSearchPredicateFactory {

	private final ElasticsearchSearchPredicateBuilderFactory factory;

	public ElasticsearchSearchPredicateFactoryImpl(SearchPredicateFactory delegate,
			ElasticsearchSearchPredicateBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public PredicateFinalStep fromJson(String jsonString) {
		return new ElasticsearchJsonStringPredicateFinalStep( factory, jsonString );
	}
}
