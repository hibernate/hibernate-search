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

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.RegexpPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;


class RegexpPredicateFieldMoreStepImpl
		implements RegexpPredicateFieldMoreStep<RegexpPredicateFieldMoreStepImpl, RegexpPredicateOptionsStep<?>>,
				AbstractBooleanMultiFieldPredicateCommonState.FieldSetState {

	private final CommonState commonState;

	private final List<RegexpPredicateBuilder> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	RegexpPredicateFieldMoreStepImpl(CommonState commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		SearchIndexScope<?> scope = commonState.scope();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( scope.fieldQueryElement( absoluteFieldPath, PredicateTypeKeys.REGEXP ) );
		}
	}

	@Override
	public RegexpPredicateFieldMoreStepImpl fields(String... absoluteFieldPaths) {
		return new RegexpPredicateFieldMoreStepImpl( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public RegexpPredicateFieldMoreStepImpl boost(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public RegexpPredicateOptionsStep<?> matching(String regexpPattern) {
		return commonState.matching( regexpPattern );
	}

	@Override
	public void contributePredicates(Consumer<SearchPredicate> collector) {
		for ( RegexpPredicateBuilder predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set state
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.build() );
		}
	}

	static class CommonState
			extends AbstractBooleanMultiFieldPredicateCommonState<CommonState, RegexpPredicateFieldMoreStepImpl>
			implements RegexpPredicateOptionsStep<CommonState> {

		CommonState(SearchPredicateDslContext<?> dslContext) {
			super( dslContext );
		}

		private RegexpPredicateOptionsStep<?> matching(String regexpPattern) {
			Contracts.assertNotNull( regexpPattern, "regexpPattern" );
			for ( RegexpPredicateFieldMoreStepImpl fieldSetState : getFieldSetStates() ) {
				for ( RegexpPredicateBuilder predicateBuilder : fieldSetState.predicateBuilders ) {
					predicateBuilder.pattern( regexpPattern );
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
