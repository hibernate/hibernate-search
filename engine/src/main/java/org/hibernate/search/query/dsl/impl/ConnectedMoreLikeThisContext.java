/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.MoreLikeThisContext;
import org.hibernate.search.query.dsl.MoreLikeThisOpenedMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTerminalMatchingContext;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMoreLikeThisContext implements MoreLikeThisContext {
	private static final String[] ALL_FIELDS = new String[0];

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final MoreLikeThisQueryContext moreLikeThisContext;

	public ConnectedMoreLikeThisContext(QueryBuildingContext context) {
		this.queryContext = context;
		this.queryCustomizer = new QueryCustomizer();
		this.moreLikeThisContext = new MoreLikeThisQueryContext();
	}

	@Override
	public MoreLikeThisContext excludeEntityUsedForComparison() {
		moreLikeThisContext.setExcludeEntityUsedForComparison( true );
		return this;
	}

	@Override
	public MoreLikeThisContext favorSignificantTermsWithFactor(float factor) {
		moreLikeThisContext.setBoostTerms( true );
		moreLikeThisContext.setTermBoostFactor( factor );
		return this;
	}

	@Override
	public MoreLikeThisTerminalMatchingContext comparingAllFields() {
		return new ConnectedMoreLikeThisMatchingContext( ALL_FIELDS, moreLikeThisContext, queryCustomizer, queryContext );
	}

	@Override
	public MoreLikeThisOpenedMatchingContext comparingFields(String... fieldNames) {
		return new ConnectedMoreLikeThisMatchingContext( fieldNames, moreLikeThisContext, queryCustomizer, queryContext );
	}

	@Override
	public MoreLikeThisOpenedMatchingContext comparingField(String fieldName) {
		return new ConnectedMoreLikeThisMatchingContext(
				new String[] {fieldName},
				moreLikeThisContext,
				queryCustomizer, queryContext );
	}

	@Override
	public MoreLikeThisContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public MoreLikeThisContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public MoreLikeThisContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
