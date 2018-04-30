/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;


/**
 * @author Guillaume Smet
 */
class BooleanJunctionPredicateBuilderImpl extends AbstractSearchPredicateBuilder
		implements BooleanJunctionPredicateBuilder<LuceneSearchPredicateCollector> {

	private final BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

	private boolean hasMustNot = false;
	private boolean hasOnlyMustNot = true;

	@Override
	public LuceneSearchPredicateCollector getMustCollector() {
		return this::must;
	}

	@Override
	public LuceneSearchPredicateCollector getMustNotCollector() {
		return this::mustNot;
	}

	@Override
	public LuceneSearchPredicateCollector getShouldCollector() {
		return this::should;
	}

	@Override
	public LuceneSearchPredicateCollector getFilterCollector() {
		return this::filter;
	}

	private void must(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.MUST );
		hasOnlyMustNot = false;
	}

	private void mustNot(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.MUST_NOT );
		hasMustNot = true;
	}

	private void should(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.SHOULD );
		hasOnlyMustNot = false;
	}

	private void filter(Query luceneQuery) {
		booleanQueryBuilder.add( luceneQuery, Occur.FILTER );
		hasOnlyMustNot = false;
	}

	@Override
	protected Query buildQuery() {
		if ( hasMustNot && hasOnlyMustNot ) {
			booleanQueryBuilder.add( new MatchAllDocsQuery(), Occur.FILTER );
		}
		return booleanQueryBuilder.build();
	}
}
