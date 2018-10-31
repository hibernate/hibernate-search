/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateFactoryContextImpl;


public class ElasticsearchSearchPredicateFactoryContextImpl
		extends DelegatingSearchPredicateFactoryContextImpl
		implements ElasticsearchSearchPredicateFactoryContext {

	private final ElasticsearchSearchPredicateBuilderFactory factory;

	public ElasticsearchSearchPredicateFactoryContextImpl(SearchPredicateFactoryContext delegate,
			ElasticsearchSearchPredicateBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public SearchPredicateTerminalContext fromJsonString(String jsonString) {
		return new JsonStringPredicateContextImpl( factory, jsonString );
	}
}
