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
import org.hibernate.search.engine.search.dsl.predicate.WildcardPredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class WildcardPredicateFieldSetContextImpl<B>
		implements WildcardPredicateFieldSetContext, AbstractBooleanMultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<WildcardPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	WildcardPredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.wildcard( absoluteFieldPath ) );
		}
	}

	@Override
	public WildcardPredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new WildcardPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public WildcardPredicateFieldSetContext boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public SearchPredicateTerminalContext matching(String wildcard) {
		return commonState.matching( wildcard );
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( WildcardPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<B, WildcardPredicateFieldSetContextImpl<B>>
			implements SearchPredicateTerminalContext {

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		private SearchPredicateTerminalContext matching(String wildcardPattern) {
			if ( wildcardPattern == null ) {
				throw log.wildcardPredicateCannotMatchNullPattern( collectAbsoluteFieldPaths() );
			}
			for ( WildcardPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( WildcardPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.pattern( wildcardPattern );

					// Fieldset contexts won't be accessed anymore, it's time to apply their options
					applyBoostAndConstantScore( fieldSetContext.fieldSetBoost, predicateBuilder );
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
