/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class LuceneObjectExistsPredicate extends AbstractLuceneSingleFieldPredicate {

	private final List<LuceneSearchPredicate> children;

	private LuceneObjectExistsPredicate(Builder builder) {
		super( builder );
		children = builder.children;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.children = null;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {
		// if exists at least one not-null field, exists on object field should match
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( LuceneSearchPredicate child : children ) {
			builder.add( child.toQuery( context ), BooleanClause.Occur.SHOULD );
		}
		return builder.build();
	}

	public static class Builder extends AbstractBuilder implements ExistsPredicateBuilder {
		private List<LuceneSearchPredicate> children = new ArrayList<>();

		public Builder(LuceneSearchContext searchContext, String absoluteFieldPath,
				List<String> nestedPathHierarchy) {
			super( searchContext, absoluteFieldPath, nestedPathHierarchy );
		}

		public void addChild(SearchPredicate child) {
			children.add( LuceneSearchPredicate.from( searchContext, child ) );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneObjectExistsPredicate( this );
		}
	}
}
