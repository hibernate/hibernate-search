/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

class LuceneMatchAllPredicate extends AbstractLuceneSearchPredicate {

	private LuceneMatchAllPredicate(Builder builder) {
		super( builder );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		return new MatchAllDocsQuery();
	}

	static class Builder extends AbstractBuilder implements MatchAllPredicateBuilder {
		Builder(LuceneSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneMatchAllPredicate( this );
		}
	}
}
