/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedPredicateDefinitionContext;

import org.apache.lucene.search.Query;

public class LuceneNamedPredicate extends AbstractLuceneSingleFieldPredicate {

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

	public static class Factory
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<NamedPredicateBuilder> {
		private final PredicateDefinition definition;
		private final String predicateName;

		public Factory(PredicateDefinition definition, String predicateName) {
			this.definition = definition;
			this.predicateName = predicateName;
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			super.checkCompatibleWith( other );
			Factory castedOther = (Factory) other;
			if ( !definition.equals( castedOther.definition ) ) {
				throw QueryLog.INSTANCE.differentPredicateDefinitionForQueryElement( definition, castedOther.definition );
			}
		}

		@Override
		public NamedPredicateBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			return new Builder( definition, predicateName, scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private final PredicateDefinition definition;
		private final String predicateName;
		private final LuceneSearchIndexCompositeNodeContext field;
		private TypedSearchPredicateFactory<?> factory;
		private final Map<String, Object> params = new LinkedHashMap<>();

		Builder(PredicateDefinition definition, String predicateName, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			super( scope, node );
			this.definition = definition;
			this.predicateName = predicateName;
			this.field = node;
		}

		@Override
		public void factory(TypedSearchPredicateFactory<?> factory) {
			this.factory = factory;
		}

		@Override
		public void param(String name, Object value) {
			params.put( name, value );
		}

		@Override
		public SearchPredicate build() {
			NamedValuesBasedPredicateDefinitionContext<?> ctx =
					new NamedValuesBasedPredicateDefinitionContext<>( factory, params,
							name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			LuceneSearchPredicate providedPredicate = LuceneSearchPredicate.from( scope, definition.create( ctx ) );

			return new LuceneNamedPredicate( this, providedPredicate );
		}
	}
}
