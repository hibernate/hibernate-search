/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNamedPredicateNode;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateFactoryContext;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;

class LuceneNamedPredicate extends AbstractLuceneSingleFieldPredicate {

	private final LuceneSearchPredicate buildPredicate;

	private LuceneNamedPredicate(Builder builder) {
		super( builder );
		buildPredicate = builder.buildPredicate;
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
	protected Query doToQuery(PredicateRequestContext context) {
		return buildPredicate.toQuery( context );
	}

	static class Builder extends AbstractBuilder implements NamedPredicateBuilder {
		private Map<String, Object> params = new LinkedHashMap<>();
		private LuceneIndexSchemaNamedPredicateNode namedPredicate;
		private LuceneSearchPredicate buildPredicate;
		private final SearchPredicateFactory predicateFactory;

		Builder(LuceneSearchContext searchContext, SearchPredicateFactory predicateFactory,
				LuceneIndexSchemaNamedPredicateNode namedPredicate) {
			super( searchContext, namedPredicate.absoluteNamedPredicatePath(), namedPredicate.parent().nestedPathHierarchy() );
			this.namedPredicate = namedPredicate;
			this.predicateFactory = predicateFactory;
		}

		@Override
		public void param(String name, Object value) {
			params.put( name, value );
		}

		@Override
		public SearchPredicate build() {
			LuceneNamedPredicateFactoryContext ctx = new LuceneNamedPredicateFactoryContext(
					namedPredicate,
					predicateFactory, params );

			NamedPredicateFactory namedPredicateFactory = (NamedPredicateFactory) namedPredicate.factory();

			buildPredicate = (LuceneSearchPredicate) namedPredicateFactory.create( ctx );

			return new LuceneNamedPredicate( this );
		}
	}

	public static class LuceneNamedPredicateFactoryContext implements NamedPredicateFactoryContext {

		private final SearchPredicateFactory predicate;
		private final LuceneIndexSchemaNamedPredicateNode namedPredicate;
		private final Map<String, Object> params;

		public LuceneNamedPredicateFactoryContext(LuceneIndexSchemaNamedPredicateNode namedPredicate,
				SearchPredicateFactory predicate, Map<String, Object> params) {
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
		public String resolvePath(String relativeFieldName) {
			return namedPredicate.parent().absolutePath( relativeFieldName );
		}
	}
}
