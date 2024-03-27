/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public class KnnPredicateFieldStepImpl
		extends AbstractPredicateFinalStep
		implements KnnPredicateFieldStep, KnnPredicateVectorStep, KnnPredicateOptionsStep {

	private final SearchPredicateFactory factory;
	private final int k;
	private BooleanPredicateBuilder booleanBuilder;
	protected KnnPredicateBuilder builder;

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

	@Override
	public KnnPredicateOptionsStep requiredMinimumSimilarity(float similarity) {
		this.builder.requiredMinimumSimilarity( similarity );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep boost(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep constantScore() {
		this.builder.constantScore();
		return this;
	}

	private BooleanPredicateBuilder booleanPredicateBuilder() {
		if ( this.booleanBuilder == null ) {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}
		return this.booleanBuilder;
	}

}
