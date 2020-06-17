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
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSingleFieldPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class LuceneExistsCompositePredicateBuilder extends AbstractLuceneSingleFieldPredicateBuilder
		implements ExistsPredicateBuilder {

	private final List<LuceneSearchPredicate> children = new ArrayList<>();

	public LuceneExistsCompositePredicateBuilder(LuceneSearchContext searchContext, String absoluteFieldPath,
			List<String> nestedPathHierarchy) {
		super( searchContext, absoluteFieldPath, nestedPathHierarchy );
	}

	@Override
	protected Query doBuild(PredicateRequestContext context) {
		// if exists at least one not-null field, exists on object field should match
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( LuceneSearchPredicate child : children ) {
			builder.add( child.toQuery( context ), BooleanClause.Occur.SHOULD );
		}
		return builder.build();
	}

	public void addChild(SearchPredicate child) {
		children.add( LuceneSearchPredicate.from( searchContext, child ) );
	}
}
