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
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.impl.Contracts;


class WildcardPredicateFieldMoreStepImpl
		implements WildcardPredicateFieldMoreStep<WildcardPredicateFieldMoreStepImpl, WildcardPredicateOptionsStep<?>>,
				AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<WildcardPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	WildcardPredicateFieldMoreStepImpl(CommonState commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchPredicateBuilderFactory<?> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.wildcard( absoluteFieldPath ) );
		}
	}

	@Override
	public WildcardPredicateFieldMoreStepImpl fields(String... absoluteFieldPaths) {
		return new WildcardPredicateFieldMoreStepImpl( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public WildcardPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public WildcardPredicateOptionsStep<?> matching(String wildcardPattern) {
		return commonState.matching( wildcardPattern );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( WildcardPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, WildcardPredicateFieldMoreStepImpl>
			implements WildcardPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private WildcardPredicateOptionsStep<?> matching(String wildcardPattern) {
			Contracts.assertNotNull( wildcardPattern, "wildcardPattern" );
			for ( WildcardPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( WildcardPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.pattern( wildcardPattern );
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
