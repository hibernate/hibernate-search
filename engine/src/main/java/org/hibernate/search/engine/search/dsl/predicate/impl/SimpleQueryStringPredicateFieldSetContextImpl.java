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
import java.util.stream.Collectors;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateFieldSetContext;
import org.hibernate.search.engine.search.dsl.predicate.SimpleQueryStringPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class SimpleQueryStringPredicateFieldSetContextImpl<B>
		implements SimpleQueryStringPredicateFieldSetContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<SimpleQueryStringPredicateBuilder.FieldContext> fieldContexts = new ArrayList<>();

	SimpleQueryStringPredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			fieldContexts.add( commonState.field( absoluteFieldPath ) );
		}
	}

	@Override
	public SimpleQueryStringPredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new SimpleQueryStringPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public SimpleQueryStringPredicateFieldSetContext boostedTo(float boost) {
		fieldContexts.forEach( c -> c.boost( boost ) );
		return this;
	}

	@Override
	public SimpleQueryStringPredicateTerminalContext matching(String simpleQueryString) {
		return commonState.matching( simpleQueryString );
	}

	static class CommonState<B> extends AbstractSearchPredicateTerminalContext<B>
			implements SimpleQueryStringPredicateTerminalContext {

		private final SimpleQueryStringPredicateBuilder<B> builder;

		private final List<SimpleQueryStringPredicateFieldSetContextImpl<B>> fieldSetContexts = new ArrayList<>();

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
			this.builder = factory.simpleQueryString();
		}

		@Override
		protected B toImplementation() {
			return builder.toImplementation();
		}

		void withConstantScore() {
			builder.withConstantScore();
		}

		void boost(float boost) {
			builder.boost( boost );
		}

		void add(SimpleQueryStringPredicateFieldSetContextImpl<B> fieldSetContext) {
			fieldSetContexts.add( fieldSetContext );
		}

		SimpleQueryStringPredicateBuilder.FieldContext field(String absoluteFieldPath) {
			return builder.field( absoluteFieldPath );
		}

		private SimpleQueryStringPredicateTerminalContext matching(String simpleQueryString) {
			if ( simpleQueryString == null ) {
				throw log.simpleQueryStringCannotBeNull( collectAbsoluteFieldPaths() );
			}
			builder.simpleQueryString( simpleQueryString );
			return this;
		}

		@Override
		public SimpleQueryStringPredicateTerminalContext withAndAsDefaultOperator() {
			builder.withAndAsDefaultOperator();
			return this;
		}

		private List<String> collectAbsoluteFieldPaths() {
			return fieldSetContexts.stream().flatMap( f -> f.absoluteFieldPaths.stream() )
					.collect( Collectors.toList() );
		}
	}

}
