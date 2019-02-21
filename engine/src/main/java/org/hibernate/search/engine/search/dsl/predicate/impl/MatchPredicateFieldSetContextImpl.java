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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.spi.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class MatchPredicateFieldSetContextImpl<B>
		implements MatchPredicateFieldSetContext, AbstractMultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<MatchPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	MatchPredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths, DslConverter dslConverter) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.match( absoluteFieldPath, dslConverter ) );
		}
	}

	@Override
	public MatchPredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ), DslConverter.ENABLED );
	}

	@Override
	public MatchPredicateFieldSetContext orRawFields(String... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ), DslConverter.DISABLED );
	}

	@Override
	public MatchPredicateFieldSetContext boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public SearchPredicateTerminalContext matching(Object value) {
		return commonState.matching( value );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( MatchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractMultiFieldPredicateCommonState<B, MatchPredicateFieldSetContextImpl<B>>
			implements SearchPredicateTerminalContext {

		private Integer maxEditDistance;
		private Integer exactPrefixLength;

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		void fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw log.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw log.invalidExactPrefixLength( exactPrefixLength );
			}
			this.maxEditDistance = maxEditDistance;
			this.exactPrefixLength = exactPrefixLength;
		}

		SearchPredicateTerminalContext matching(Object value) {
			if ( value == null ) {
				throw log.matchPredicateCannotMatchNullValue( collectAbsoluteFieldPaths() );
			}

			for ( MatchPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.value( value );

					// Fieldset contexts won't be accessed anymore, it's time to apply their options
					applyBoostAndConstantScore( fieldSetContext.fieldSetBoost, predicateBuilder );
					if ( maxEditDistance != null ) {
						predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
					}
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
