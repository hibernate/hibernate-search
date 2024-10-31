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
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedValuesBasedPredicateDefinitionContext;

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
			NamedValuesBasedPredicateDefinitionContext ctx = new NamedValuesBasedPredicateDefinitionContext( factory, params,
					name -> QueryLog.INSTANCE.paramNotDefined( name, predicateName, field.eventContext() ) );

			ElasticsearchSearchPredicate providedPredicate = ElasticsearchSearchPredicate.from(
					scope, definition.create( ctx ) );

			return new ElasticsearchNamedPredicate( this, providedPredicate );
		}
	}
}
