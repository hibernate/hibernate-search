/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNamedPredicateNode;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProviderContext;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;

class ElasticsearchNamedPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private final ElasticsearchSearchPredicate buildPredicate;

	private ElasticsearchNamedPredicate(Builder builder) {
		super( builder );
		this.buildPredicate = builder.buildPredicate;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.namedPredicate = null;
		builder.params = null;
		builder.buildPredicate = null;
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		buildPredicate.checkNestableWithin( expectedParentNestedPath );
		super.checkNestableWithin( expectedParentNestedPath );
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		return buildPredicate.toJsonQuery( context );
	}

	static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private Map<String, Object> params = new LinkedHashMap<>();
		private ElasticsearchIndexSchemaNamedPredicateNode namedPredicate;
		private ElasticsearchSearchPredicate buildPredicate;
		private final SearchPredicateFactory predicateFactory;

		Builder(ElasticsearchSearchContext searchContext, SearchPredicateFactory predicateFactory,
				ElasticsearchIndexSchemaNamedPredicateNode filter) {
			super( searchContext, filter.absoluteNamedPredicatePath(), filter.parent().nestedPathHierarchy() );
			this.namedPredicate = filter;
			this.predicateFactory = predicateFactory;
		}

		@Override
		public void param(String name, Object value) {
			params.put( name, value );
		}

		@Override
		public SearchPredicate build() {
			ElasticsearchNamedPredicateProviderContext ctx = new ElasticsearchNamedPredicateProviderContext( namedPredicate,
					predicateFactory,
					params );

			NamedPredicateProvider namedPredicateProvider = namedPredicate.provider();

			buildPredicate = ElasticsearchSearchPredicate.from( searchContext, namedPredicateProvider.create( ctx ) );

			return new ElasticsearchNamedPredicate( this );
		}
	}

	public static class ElasticsearchNamedPredicateProviderContext implements NamedPredicateProviderContext {

		private final ElasticsearchIndexSchemaNamedPredicateNode namedPredicate;
		private final SearchPredicateFactory predicate;
		private final Map<String, Object> params;

		public ElasticsearchNamedPredicateProviderContext(ElasticsearchIndexSchemaNamedPredicateNode namedPredicate,
				SearchPredicateFactory predicate,
				Map<String, Object> params) {
			this.namedPredicate = namedPredicate;
			this.predicate = predicate;
			this.params = params;
		}

		@Override
		public SearchPredicateFactory predicate() {
			return predicate;
		}

		@Override
		public Object param(String name) {
			return params.get( name );
		}

		@Override
		public String absolutePath(String relativeFieldPath) {
			return namedPredicate.parent().absolutePath( relativeFieldPath );
		}
	}
}
