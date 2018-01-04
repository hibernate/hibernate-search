/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.ElasticsearchSearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.spi.SearchDslContext;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchPredicateContainerContextImpl<N>
		extends DelegatingSearchPredicateContainerContextImpl<N>
		implements ElasticsearchSearchPredicateContainerContext<N> {

	private final ElasticsearchSearchTargetContext targetContext;

	private final SearchDslContext<N, ElasticsearchSearchPredicateCollector> dslContext;

	public ElasticsearchSearchPredicateContainerContextImpl(SearchPredicateContainerContext<N> delegate,
			ElasticsearchSearchTargetContext targetContext,
			SearchDslContext<N, ElasticsearchSearchPredicateCollector> dslContext) {
		super( delegate );
		this.targetContext = targetContext;
		this.dslContext = dslContext;
	}

	@Override
	public N fromJsonString(String jsonString) {
		dslContext.addContributor( new UserProvidedJsonPredicateContributor( jsonString ) );
		return dslContext.getNextContext();
	}
}
