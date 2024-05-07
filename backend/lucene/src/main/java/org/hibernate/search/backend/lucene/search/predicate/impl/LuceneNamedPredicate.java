/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedPredicateDefinitionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public class LuceneNamedPredicate extends AbstractLuceneSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchPredicate instance;

	private LuceneNamedPredicate(Builder builder, LuceneSearchPredicate providedPredicate) {
		super( builder );
		instance = providedPredicate;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		instance.checkNestableWithin( expectedParentNestedPath );
		super.checkNestableWithin( expectedParentNestedPath );
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return instance.toQuery( context );
	}

	public static class Factory<E>
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<NamedPredicateBuilder<E>> {
		private final PredicateDefinition<E> definition;
		private final String predicateName;

		public Factory(PredicateDefinition<E> definition, String predicateName) {
			this.definition = definition;
			this.predicateName = predicateName;
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			super.checkCompatibleWith( other );
			Factory<E> castedOther = (Factory<E>) other;
			if ( !definition.equals( castedOther.definition ) ) {
				throw log.differentPredicateDefinitionForQueryElement( definition, castedOther.definition );
			}
		}

		@Override
		public NamedPredicateBuilder<E> create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			return new Builder<>( definition, predicateName, scope, node );
		}
	}

	private static class Builder<E> extends AbstractBuilder implements NamedPredicateBuilder<E> {
		private final PredicateDefinition<E> definition;
		private final String predicateName;
		private final LuceneSearchIndexCompositeNodeContext field;
		private SearchPredicateFactory<E> factory;
		private final Map<String, Object> params = new LinkedHashMap<>();

		Builder(PredicateDefinition<E> definition, String predicateName, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			super( scope, node );
			this.definition = definition;
			this.predicateName = predicateName;
			this.field = node;
		}

		@Override
		public void factory(SearchPredicateFactory<E> factory) {
			this.factory = factory;
		}

		@Override
		public void param(String name, Object value) {
			params.put( name, value );
		}

		@Override
		public SearchPredicate build() {
			NamedValuesBasedPredicateDefinitionContext<E> ctx = new NamedValuesBasedPredicateDefinitionContext<>( factory, params,
					name -> log.paramNotDefined( name, predicateName, field.eventContext() ) );

			LuceneSearchPredicate providedPredicate = LuceneSearchPredicate.from( scope, definition.create( ctx ) );

			return new LuceneNamedPredicate( this, providedPredicate );
		}
	}
}
