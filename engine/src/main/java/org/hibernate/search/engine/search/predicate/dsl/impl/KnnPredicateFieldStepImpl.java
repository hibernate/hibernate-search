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
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorGenericStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateVectorStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.reference.traits.predicate.KnnPredicateFieldReference;

public class KnnPredicateFieldStepImpl<E>
		extends AbstractPredicateFinalStep
		implements KnnPredicateFieldStep<E>, KnnPredicateVectorStep, KnnPredicateOptionsStep<E> {

	private final SearchPredicateFactory<E> factory;
	private final int k;
	private BooleanPredicateBuilder booleanBuilder;
	protected KnnPredicateBuilder builder;

	public KnnPredicateFieldStepImpl(SearchPredicateFactory<E> factory, SearchPredicateDslContext<?> dslContext, int k) {
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
	public <T> KnnPredicateVectorGenericStep<T> field(KnnPredicateFieldReference<E, T> field) {
		this.field( field.absolutePath() );
		return new KnnPredicateVectorGenericStepImpl<>();
	}

	@Override
	public KnnPredicateOptionsStep<E> filter(SearchPredicate searchPredicate) {
		this.booleanPredicateBuilder().must( searchPredicate );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> filter(
			Function<? super SearchPredicateFactory<E>, ? extends PredicateFinalStep> clauseContributor) {
		this.booleanPredicateBuilder().must( clauseContributor.apply( factory ).toPredicate() );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> matching(byte... vector) {
		this.builder.vector( vector );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> matching(float... vector) {
		this.builder.vector( vector );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> requiredMinimumSimilarity(float similarity) {
		this.builder.requiredMinimumSimilarity( similarity );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> boost(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	public KnnPredicateOptionsStep<E> constantScore() {
		this.builder.constantScore();
		return this;
	}

	@Override
	protected SearchPredicate build() {
		if ( this.booleanBuilder != null ) {
			builder.filter( booleanBuilder.build() );
		}
		return builder.build();
	}

	private BooleanPredicateBuilder booleanPredicateBuilder() {
		if ( this.booleanBuilder == null ) {
			this.booleanBuilder = dslContext.scope().predicateBuilders().bool();
		}
		return this.booleanBuilder;
	}

	private class KnnPredicateVectorGenericStepImpl<T> implements KnnPredicateVectorGenericStep<T> {

		@Override
		public KnnPredicateOptionsStep<E> matching(T vector) {
			KnnPredicateFieldStepImpl.this.builder.vector( vector );
			return KnnPredicateFieldStepImpl.this;
		}
	}

}
