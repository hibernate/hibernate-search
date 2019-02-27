/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class PhrasePredicateFieldSetContextImpl<B>
		implements PhrasePredicateFieldSetContext, AbstractBooleanMultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<PhrasePredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	PhrasePredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.phrase( absoluteFieldPath ) );
		}
	}

	@Override
	public PhrasePredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new PhrasePredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public PhrasePredicateFieldSetContext boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public SearchPredicateTerminalContext matching(String phrase) {
		return commonState.matching( phrase );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( PhrasePredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<B, PhrasePredicateFieldSetContextImpl<B>>
			implements SearchPredicateTerminalContext {

		private int slop = 0;

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		void withSlop(int slop) {
			if ( slop < 0 ) {
				throw log.invalidPhrasePredicateSlop( slop );
			}
			this.slop = slop;
		}

		private SearchPredicateTerminalContext matching(String phrase) {
			if ( phrase == null ) {
				throw log.phrasePredicateCannotMatchNullPhrase( collectAbsoluteFieldPaths() );
			}

			for ( PhrasePredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( PhrasePredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.phrase( phrase );

					// Fieldset contexts won't be accessed anymore, it's time to apply their options
					applyBoostAndConstantScore( fieldSetContext.fieldSetBoost, predicateBuilder );
					predicateBuilder.slop( slop );
				}
			}
			return this;
		}

		private List<String> collectAbsoluteFieldPaths() {
			return getFieldSetContexts().stream().flatMap( f -> f.absoluteFieldPaths.stream() )
					.collect( Collectors.toList() );
		}

	}

}
