/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;


public class ElasticsearchSearchPredicateContainerContextImpl
		extends DelegatingSearchPredicateContainerContextImpl
		implements ElasticsearchSearchPredicateContainerContext {

	private final ElasticsearchSearchPredicateFactory factory;

	public ElasticsearchSearchPredicateContainerContextImpl(SearchPredicateContainerContext delegate,
			ElasticsearchSearchPredicateFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public SearchPredicateTerminalContext fromJsonString(String jsonString) {
		return new JsonStringPredicateContextImpl( factory, jsonString );
	}
}
