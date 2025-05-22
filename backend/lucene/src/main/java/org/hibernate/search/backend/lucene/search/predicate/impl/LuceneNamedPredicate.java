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
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.TypedPredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateFactoryDelegate;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedPredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedTypedPredicateDefinitionContext;

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
			return new BasicBuilder( definition, predicateName, scope, node );
		}
	}

	public static class TypedFactory<SR>
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<NamedPredicateBuilder> {
		private final TypedPredicateDefinition<SR> definition;
		private final String predicateName;

		public TypedFactory(TypedPredicateDefinition<SR> definition, String predicateName) {
			this.definition = definition;
			this.predicateName = predicateName;
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			super.checkCompatibleWith( other );
			TypedFactory castedOther = (TypedFactory) other;
			if ( !definition.equals( castedOther.definition ) ) {
				throw QueryLog.INSTANCE.differentPredicateDefinitionForQueryElement( definition, castedOther.definition );
			}
		}

		@Override
		public NamedPredicateBuilder create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			return new TypedBuilder<>( definition, predicateName, scope, node );
		}
	}

	private abstract static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		protected final String predicateName;
		protected final LuceneSearchIndexCompositeNodeContext field;
		protected final Map<String, Object> params = new LinkedHashMap<>();

		Builder(String predicateName, LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			super( scope, node );
			this.predicateName = predicateName;
			this.field = node;
		}

		@Override
		public final void param(String name, Object value) {
			params.put( name, value );
		}

		protected abstract LuceneSearchPredicate providedPredicate();

		@Override
		public final SearchPredicate build() {
			return new LuceneNamedPredicate( this, providedPredicate() );
		}
	}

	private static class BasicBuilder extends Builder {
		private final PredicateDefinition definition;
		private SearchPredicateFactory factory;

		BasicBuilder(PredicateDefinition definition, String predicateName, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			super( predicateName, scope, node );
			this.definition = definition;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void factory(ExtendedSearchPredicateFactory<?, ?> factory) {
			this.factory = new SearchPredicateFactoryDelegate( factory.withScopeRoot( NonStaticMetamodelScope.class ) );
		}

		@Override
		protected LuceneSearchPredicate providedPredicate() {
			NamedValuesBasedPredicateDefinitionContext ctx =
					new NamedValuesBasedPredicateDefinitionContext( factory, params,
							name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			return LuceneSearchPredicate.from( scope, definition.create( ctx ) );
		}
	}

	private static class TypedBuilder<SR> extends Builder {
		private final TypedPredicateDefinition<SR> definition;
		private TypedSearchPredicateFactory<SR> factory;

		TypedBuilder(TypedPredicateDefinition<SR> definition, String predicateName, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexCompositeNodeContext node) {
			super( predicateName, scope, node );
			this.definition = definition;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void factory(ExtendedSearchPredicateFactory<?, ?> factory) {
			this.factory = factory.withScopeRoot( definition.scopeRootType() );
		}

		@Override
		protected LuceneSearchPredicate providedPredicate() {
			var ctx = new NamedValuesBasedTypedPredicateDefinitionContext<>( factory, params,
					name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			return LuceneSearchPredicate.from( scope, definition.create( ctx ) );
		}
	}
}
