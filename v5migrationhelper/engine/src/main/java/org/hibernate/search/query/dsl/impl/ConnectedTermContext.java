/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.FuzzyContext;
import org.hibernate.search.query.dsl.TermContext;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.query.dsl.WildcardContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedTermContext implements TermContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;

	public ConnectedTermContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
		this.termContext = new TermQueryContext( TermQueryContext.Approximation.EXACT );
	}

	@Override
	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext( termContext, field, queryCustomizer, queryContext );
	}

	@Override
	public TermMatchingContext onFields(String... fields) {
		return new ConnectedTermMatchingContext( termContext, fields, queryCustomizer, queryContext );
	}

	@Override
	public FuzzyContext fuzzy() {
		return new ConnectedFuzzyContext( queryCustomizer, queryContext );
	}

	@Override
	public WildcardContext wildcard() {
		return new ConnectedWildcardContext( queryCustomizer, queryContext );
	}

	@Override
	public ConnectedTermContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public ConnectedTermContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public ConnectedTermContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
