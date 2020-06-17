/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;

import com.google.gson.JsonObject;

final class ElasticsearchJsonPredicateFinalStep
		extends AbstractPredicateFinalStep
		implements PredicateFinalStep {
	private final ElasticsearchSearchPredicateBuilder builder;

	ElasticsearchJsonPredicateFinalStep(ElasticsearchSearchPredicateBuilderFactory factory, JsonObject jsonObject) {
		super( factory );
		this.builder = factory.fromJson( jsonObject );
	}

	ElasticsearchJsonPredicateFinalStep(ElasticsearchSearchPredicateBuilderFactory factory, String jsonString) {
		super( factory );
		this.builder = factory.fromJson( jsonString );
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
