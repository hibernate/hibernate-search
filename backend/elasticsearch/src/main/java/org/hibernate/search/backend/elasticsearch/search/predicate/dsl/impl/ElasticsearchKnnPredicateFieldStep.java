/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchKnnPredicateOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.predicate.spi.ElasticsearchKnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractKnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class ElasticsearchKnnPredicateFieldStep
		extends AbstractKnnPredicateFieldStep<ElasticsearchKnnPredicateOptionsStep<?>, ElasticsearchKnnPredicateBuilder>
		implements
		ElasticsearchKnnPredicateOptionsStep<ElasticsearchKnnPredicateOptionsStep<?>> {

	public ElasticsearchKnnPredicateFieldStep(SearchPredicateFactory factory, SearchPredicateDslContext<?> dslContext,
			int k) {
		super( factory, dslContext, k );
	}

	@Override
	protected ElasticsearchKnnPredicateBuilder createBuilder(String fieldPath) {
		return (ElasticsearchKnnPredicateBuilder) dslContext.scope().fieldQueryElement( fieldPath, PredicateTypeKeys.KNN );
	}

	@Override
	public ElasticsearchKnnPredicateOptionsStep<?> numberOfCandidates(int numberOfCandidates) {
		this.builder.numberOfCandidates( numberOfCandidates );
		return this;
	}

	@Override
	protected ElasticsearchKnnPredicateOptionsStep<?> thisAsT() {
		return this;
	}
}
