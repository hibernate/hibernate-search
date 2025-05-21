/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.TypedPredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateFactoryDelegate;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedPredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedTypedPredicateDefinitionContext;

import com.google.gson.JsonObject;

public class ElasticsearchNamedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private final ElasticsearchSearchPredicate providedPredicate;

	private ElasticsearchNamedPredicate(Builder builder, ElasticsearchSearchPredicate providedPredicate) {
		super( builder );
		this.providedPredicate = providedPredicate;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		providedPredicate.checkNestableWithin( context );
		super.checkNestableWithin( context );
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		return providedPredicate.toJsonQuery( context );
	}

	public static class Factory
			extends AbstractElasticsearchCompositeNodeSearchQueryElementFactory<NamedPredicateBuilder> {
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
		public NamedPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new BasicBuilder( definition, predicateName, scope, node );
		}
	}

	public static class TypedFactory<SR>
			extends AbstractElasticsearchCompositeNodeSearchQueryElementFactory<NamedPredicateBuilder> {
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
		public NamedPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new TypedBuilder<>( definition, predicateName, scope, node );
		}
	}

	private abstract static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		protected final String predicateName;
		protected final ElasticsearchSearchIndexCompositeNodeContext field;
		protected final Map<String, Object> params = new LinkedHashMap<>();

		Builder(String predicateName,
				ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			super( scope, node );
			this.predicateName = predicateName;
			this.field = node;
		}

		@Override
		public final void param(String name, Object value) {
			params.put( name, value );
		}

		protected abstract ElasticsearchSearchPredicate providedPredicate();

		@Override
		public final SearchPredicate build() {
			return new ElasticsearchNamedPredicate( this, providedPredicate() );
		}

	}

	private static class BasicBuilder extends Builder {
		private final PredicateDefinition definition;
		private SearchPredicateFactory factory;

		BasicBuilder(PredicateDefinition definition, String predicateName,
				ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			super( predicateName, scope, node );
			this.definition = definition;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void factory(TypedSearchPredicateFactory<?> factory) {
			if ( isCompatible( factory ) ) {
				this.factory =
						new SearchPredicateFactoryDelegate( (TypedSearchPredicateFactory<NonStaticMetamodelScope>) factory );
			}
			else {
				throw new IllegalArgumentException( "Current search predicate factory is incompatible with this \""
						+ predicateName + "\" name predicate." );
			}
		}

		private boolean isCompatible(TypedSearchPredicateFactory<?> factory) {
			return true;
		}

		@Override
		protected ElasticsearchSearchPredicate providedPredicate() {
			NamedValuesBasedPredicateDefinitionContext ctx =
					new NamedValuesBasedPredicateDefinitionContext( factory, params,
							name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			return ElasticsearchSearchPredicate.from( scope, definition.create( ctx ) );
		}
	}

	private static class TypedBuilder<SR> extends Builder {
		private final TypedPredicateDefinition<SR> definition;
		private TypedSearchPredicateFactory<SR> factory;

		TypedBuilder(TypedPredicateDefinition<SR> definition, String predicateName, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			super( predicateName, scope, node );
			this.definition = definition;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void factory(TypedSearchPredicateFactory<?> factory) {
			if ( isCompatible( factory ) ) {
				this.factory = (TypedSearchPredicateFactory<SR>) factory;
			}
			else {
				throw new IllegalArgumentException( "Current search predicate factory is incompatible with this \""
						+ predicateName + "\" name predicate." );
			}
		}

		private boolean isCompatible(TypedSearchPredicateFactory<?> factory) {
			return true;
		}

		@Override
		protected ElasticsearchSearchPredicate providedPredicate() {
			var ctx = new NamedValuesBasedTypedPredicateDefinitionContext<>( factory, params,
					name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			return ElasticsearchSearchPredicate.from( scope, definition.create( ctx ) );
		}
	}
}
