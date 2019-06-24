/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;

final class ElasticsearchJsonStringPredicateFinalStep
		extends AbstractPredicateFinalStep<ElasticsearchSearchPredicateBuilder>
		implements PredicateFinalStep {
	private final ElasticsearchSearchPredicateBuilder builder;

	ElasticsearchJsonStringPredicateFinalStep(ElasticsearchSearchPredicateBuilderFactory factory, String jsonString) {
		super( factory );
		this.builder = factory.fromJson( jsonString );
	}

	@Override
	protected ElasticsearchSearchPredicateBuilder toImplementation() {
		return builder;
	}
}
