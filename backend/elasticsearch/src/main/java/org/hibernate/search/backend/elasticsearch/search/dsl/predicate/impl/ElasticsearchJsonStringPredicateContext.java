/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;

final class ElasticsearchJsonStringPredicateContext
		extends AbstractSearchPredicateTerminalContext<ElasticsearchSearchPredicateBuilder>
		implements SearchPredicateTerminalContext {
	private final ElasticsearchSearchPredicateBuilder builder;

	ElasticsearchJsonStringPredicateContext(ElasticsearchSearchPredicateBuilderFactory factory, String jsonString) {
		super( factory );
		this.builder = factory.fromJsonString( jsonString );
	}

	@Override
	protected ElasticsearchSearchPredicateBuilder toImplementation() {
		return builder;
	}
}
