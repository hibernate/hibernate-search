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

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.PhrasePredicateTerminalContext;
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
	public PhrasePredicateTerminalContext matching(String phrase) {
		return commonState.matching( phrase );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( PhrasePredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Fieldset contexts won't be accessed anymore, it's time to apply their options
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, PhrasePredicateFieldSetContextImpl<B>>
			implements PhrasePredicateTerminalContext {

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		private PhrasePredicateTerminalContext matching(String phrase) {
			if ( phrase == null ) {
				throw log.phrasePredicateCannotMatchNullPhrase( getEventContext() );
			}

			for ( PhrasePredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( PhrasePredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.phrase( phrase );
				}
			}
			return this;
		}

		@Override
		public PhrasePredicateTerminalContext withSlop(int slop) {
			if ( slop < 0 ) {
				throw log.invalidPhrasePredicateSlop( slop );
			}

			for ( PhrasePredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( PhrasePredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.slop( slop );
				}
			}
			return this;
		}

		@Override
		public PhrasePredicateTerminalContext analyzer(String analyzerName) {
			for ( PhrasePredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( PhrasePredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public PhrasePredicateTerminalContext ignoreAnalyzer() {
			for ( PhrasePredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( PhrasePredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.ignoreAnalyzer();
				}
			}
			return this;
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

}
