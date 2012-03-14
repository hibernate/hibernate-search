/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.query.dsl.FuzzyContext;
import org.hibernate.search.query.dsl.TermMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedFuzzyContext implements FuzzyContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;

	public ConnectedFuzzyContext(QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryCustomizer = queryCustomizer;
		this.termContext = new TermQueryContext( TermQueryContext.Approximation.FUZZY);
		this.queryContext = queryContext;
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(termContext, field, queryCustomizer, queryContext);
	}

	public TermMatchingContext onFields(String... fields) {
		return new ConnectedTermMatchingContext(termContext, fields, queryCustomizer, queryContext);
	}

	public ConnectedFuzzyContext withThreshold(float threshold) {
		termContext.setThreshold( threshold );
		return this;
	}

	public ConnectedFuzzyContext withPrefixLength(int prefixLength) {
		termContext.setPrefixLength( prefixLength );
		return this;
	}

	public FuzzyContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public FuzzyContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public FuzzyContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy(filter);
		return this;
	}

}