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
import java.util.Optional;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProviderContext;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	public static class Factory
			extends AbstractLuceneSearchCompositeIndexSchemaElementQueryElementFactory<NamedPredicateBuilder> {
		private final NamedPredicateProvider provider;

		public Factory(NamedPredicateProvider provider) {
			this.provider = provider;
		}

		@Override
		public void checkCompatibleWith(LuceneSearchCompositeIndexSchemaElementQueryElementFactory<?> other) {
			super.checkCompatibleWith( other );
			Factory castedOther = (Factory) other;
			if ( !provider.equals( castedOther.provider ) ) {
				throw log.differentProviderForQueryElement( provider, castedOther.provider );
			}
		}

		@Override
		public NamedPredicateBuilder create(LuceneSearchContext searchContext,
				LuceneSearchCompositeIndexSchemaElementContext field) {
			return new Builder( provider, searchContext, field );
		}
	}

	private static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private final NamedPredicateProvider provider;
		private final LuceneSearchCompositeIndexSchemaElementContext field;
		private SearchPredicateFactory factory;
		private final Map<String, Object> params = new LinkedHashMap<>();

		Builder(NamedPredicateProvider provider, LuceneSearchContext searchContext,
				LuceneSearchCompositeIndexSchemaElementContext field) {
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
			LuceneNamedPredicateProviderContext ctx = new LuceneNamedPredicateProviderContext(
					factory, field, params );

			LuceneSearchPredicate providedPredicate = LuceneSearchPredicate.from( searchContext, provider.create( ctx ) );

			return new LuceneNamedPredicate( this, providedPredicate );
		}
	}

	private static class LuceneNamedPredicateProviderContext implements NamedPredicateProviderContext {

		private final SearchPredicateFactory factory;
		private final LuceneSearchCompositeIndexSchemaElementContext field;
		private final Map<String, Object> params;

		LuceneNamedPredicateProviderContext(SearchPredicateFactory factory,
				LuceneSearchCompositeIndexSchemaElementContext field, Map<String, Object> params) {
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

			Object value = params.get( name );
			if ( value == null ) {
				throw log.paramNotDefined( name );
			}
			return value;
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
