/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class LuceneExistsCompositePredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements ExistsPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final List<LuceneSearchPredicateBuilder> children = new ArrayList<>();

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		// if exists at least one not-null field, exists on object field should match
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for ( LuceneSearchPredicateBuilder child : children ) {
			builder.add( child.build( context ), BooleanClause.Occur.SHOULD );
		}
		return builder.build();
	}

	public void addChild(SearchPredicateBuilder<LuceneSearchPredicateBuilder> childBuilder) {
		children.add( childBuilder.toImplementation() );
	}
}
