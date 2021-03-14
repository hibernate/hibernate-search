/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.TermsPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;


class TermsPredicateFieldMoreStepImpl
		implements TermsPredicateFieldMoreStep<TermsPredicateFieldMoreStepImpl, TermsPredicateOptionsStep<?>>,
				AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<TermsPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	TermsPredicateFieldMoreStepImpl(CommonState commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchPredicateBuilderFactory<?> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.terms( absoluteFieldPath ) );
		}
	}

	@Override
	public TermsPredicateFieldMoreStepImpl fields(String... absoluteFieldPaths) {
		return new TermsPredicateFieldMoreStepImpl( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public TermsPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public TermsPredicateOptionsStep<?> matchingAny(Object term, Object... terms) {
		return commonState.matchingAny( term, terms );
	}

	@Override
	public TermsPredicateOptionsStep<?> matchingAll(Object term, Object... terms) {
		return commonState.matchingAll( term, terms );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( TermsPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, TermsPredicateFieldMoreStepImpl>
			implements TermsPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private TermsPredicateOptionsStep<?> matchingAny(Object term, Object ... terms) {
			Contracts.assertNotNull( term, "term" );
			for ( TermsPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( TermsPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.matchingAny( term, terms );
				}
			}
			return this;
		}

		private TermsPredicateOptionsStep<?> matchingAll(Object term, Object ... terms) {
			Contracts.assertNotNull( term, "term" );
			for ( TermsPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( TermsPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.matchingAll( term, terms );
				}
			}
			return this;
		}

		@Override
		protected CommonState thisAsS() {
			return this;
		}
	}

}
