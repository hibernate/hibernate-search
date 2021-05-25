/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Optional;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProviderContext;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ElasticsearchNamedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchPredicate providedPredicate;

	private ElasticsearchNamedPredicate(Builder builder, ElasticsearchSearchPredicate providedPredicate) {
		super( builder );
		this.providedPredicate = providedPredicate;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		providedPredicate.checkNestableWithin( expectedParentNestedPath );
		super.checkNestableWithin( expectedParentNestedPath );
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		return providedPredicate.toJsonQuery( context );
	}

	public static class Factory
			extends AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<NamedPredicateBuilder> {
		private final NamedPredicateProvider provider;

		public Factory(NamedPredicateProvider provider) {
			this.provider = provider;
		}

		@Override
		public void checkCompatibleWith(ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<?> other) {
			super.checkCompatibleWith( other );
			Factory castedOther = (Factory) other;
			if ( !provider.equals( castedOther.provider ) ) {
				throw log.differentProviderForQueryElement( provider, castedOther.provider );
			}
		}

		@Override
		public NamedPredicateBuilder create(ElasticsearchSearchContext searchContext,
				ElasticsearchSearchCompositeIndexSchemaElementContext field) {
			return new Builder( provider, searchContext, field );
		}
	}

	private static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private final NamedPredicateProvider provider;
		private final ElasticsearchSearchCompositeIndexSchemaElementContext field;
		private SearchPredicateFactory factory;
		private final Map<String, Object> params = new LinkedHashMap<>();

		Builder(NamedPredicateProvider provider, ElasticsearchSearchContext searchContext,
				ElasticsearchSearchCompositeIndexSchemaElementContext field) {
			super( searchContext, field );
			this.provider = provider;
			this.field = field;
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
			ElasticsearchNamedPredicateProviderContext ctx = new ElasticsearchNamedPredicateProviderContext(
					factory, field, params );

			ElasticsearchSearchPredicate providedPredicate = ElasticsearchSearchPredicate.from(
					searchContext, provider.create( ctx ) );

			return new ElasticsearchNamedPredicate( this, providedPredicate );
		}
	}

	private static class ElasticsearchNamedPredicateProviderContext implements NamedPredicateProviderContext {

		private final SearchPredicateFactory factory;
		private final ElasticsearchSearchCompositeIndexSchemaElementContext field;
		private final Map<String, Object> params;

		ElasticsearchNamedPredicateProviderContext(SearchPredicateFactory factory,
				ElasticsearchSearchCompositeIndexSchemaElementContext field, Map<String, Object> params) {
			this.factory = factory;
			this.field = field;
			this.params = params;
		}

		@Override
		public SearchPredicateFactory predicate() {
			return factory;
		}

		@Override
		public Object param(String name) {
			Contracts.assertNotNull( name, "name" );
			return params.get( name );
		}

		@Override
		public Optional<Object> paramOptional(String name) {
			Contracts.assertNotNull( name, "name" );
			return Optional.ofNullable( params.get( name ) );
		}

		@Override
		public String absolutePath(String relativeFieldPath) {
			Contracts.assertNotNull( relativeFieldPath, "relativeFieldPath" );
			return field.absolutePath( relativeFieldPath );
		}
	}
}
