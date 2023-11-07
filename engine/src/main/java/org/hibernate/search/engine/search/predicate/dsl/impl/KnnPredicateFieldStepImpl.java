/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class KnnPredicateFieldStepImpl extends AbstractPredicateFinalStep
		implements KnnPredicateFieldStep, KnnPredicateVectorStep, KnnPredicateOptionsStep {

	private final SearchPredicateFactory factory;
	private final int k;
	private KnnPredicateBuilder builder;
	private BooleanPredicateBuilder booleanBuilder;

	public KnnPredicateFieldStepImpl(SearchPredicateFactory factory, SearchPredicateDslContext<?> dslContext, int k) {
		super( dslContext );
		this.factory = factory;
		this.k = k;
	}

	@Override
	public KnnPredicateVectorStep field(String fieldPath) {
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, PredicateTypeKeys.KNN );
		this.builder.k( k );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		if ( this.booleanBuilder != null ) {
			builder.filter( booleanBuilder.build() );
		}
		return builder.build();
	}

	@Override
	public KnnPredicateOptionsStep filter(SearchPredicate searchPredicate) {
		this.booleanPredicateBuilder().must( searchPredicate );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep filter(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor) {
		this.booleanPredicateBuilder().must( clauseContributor.apply( factory ).toPredicate() );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep matching(byte... vector) {
		this.builder.vector( vector );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep matching(float... vector) {
		this.builder.vector( vector );
		return this;
	}

	private BooleanPredicateBuilder booleanPredicateBuilder() {
		if ( this.booleanBuilder == null ) {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}
		return this.booleanBuilder;
	}

}
