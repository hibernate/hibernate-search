/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchNamedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
				throw log.differentPredicateDefinitionForQueryElement( definition, castedOther.definition );
			}
		}

		@Override
		public NamedPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			return new Builder( definition, predicateName, scope, node );
		}
	}

	private static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private final PredicateDefinition definition;
		private final String predicateName;
		private final ElasticsearchSearchIndexCompositeNodeContext field;
		private SearchPredicateFactory factory;
		private final Map<String, Object> params = new LinkedHashMap<>();

		Builder(PredicateDefinition definition, String predicateName,
				ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexCompositeNodeContext node) {
			super( scope, node );
			this.definition = definition;
			this.predicateName = predicateName;
			this.field = node;
		}

		@Override
		public void factory(SearchPredicateFactory factory) {
			this.factory = factory;
		}

		@Override
		public void param(String name, Object value) {
			params.put( name, value );
		}

		@Override
		public SearchPredicate build() {
			ElasticsearchPredicateDefinitionContext ctx = new ElasticsearchPredicateDefinitionContext(
					factory, field, predicateName, params );

			ElasticsearchSearchPredicate providedPredicate = ElasticsearchSearchPredicate.from(
					scope, definition.create( ctx ) );

			return new ElasticsearchNamedPredicate( this, providedPredicate );
		}
	}

	private static class ElasticsearchPredicateDefinitionContext implements PredicateDefinitionContext {

		private final SearchPredicateFactory factory;
		private final ElasticsearchSearchIndexCompositeNodeContext field;
		private final String predicateName;
		private final Map<String, Object> params;

		ElasticsearchPredicateDefinitionContext(SearchPredicateFactory factory,
				ElasticsearchSearchIndexCompositeNodeContext field, String predicateName,
				Map<String, Object> params) {
			this.factory = factory;
			this.field = field;
			this.predicateName = predicateName;
			this.params = params;
		}

		@Override
		public SearchPredicateFactory predicate() {
			return factory;
		}

		@Override
		public <T> T param(String name, Class<T> paramType) {
			Contracts.assertNotNull( name, "name" );
			Contracts.assertNotNull( paramType, "paramType" );

			Object value = params.get( name );
			if ( value == null ) {
				throw log.paramNotDefined( name, predicateName, field.eventContext() );
			}
			return paramType.cast( value );
		}

		@Override
		public <T> Optional<T> paramOptional(String name, Class<T> paramType) {
			Contracts.assertNotNull( name, "name" );
			Contracts.assertNotNull( paramType, "paramType" );

			return Optional.ofNullable( params.get( name ) ).map( paramType::cast );
		}
	}
}
