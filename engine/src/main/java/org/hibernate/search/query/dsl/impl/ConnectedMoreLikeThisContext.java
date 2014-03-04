/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.MoreLikeThisContext;
import org.hibernate.search.query.dsl.MoreLikeThisOpenedMatchingContext;
import org.hibernate.search.query.dsl.MoreLikeThisTerminalMatchingContext;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
