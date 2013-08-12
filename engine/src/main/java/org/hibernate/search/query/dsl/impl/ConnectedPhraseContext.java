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

import org.hibernate.search.query.dsl.PhraseContext;
import org.hibernate.search.query.dsl.PhraseMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedPhraseContext implements PhraseContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext phraseContext;


	public ConnectedPhraseContext(QueryBuildingContext queryContext) {
		this.queryCustomizer = new QueryCustomizer();
		this.phraseContext = new PhraseQueryContext();
		this.queryContext = queryContext;
	}

	@Override
	public PhraseContext withSlop(int slop) {
		phraseContext.setSlop( slop );
		return this;
	}

	@Override
	public PhraseMatchingContext onField(String fieldName) {
		return new ConnectedPhraseMatchingContext(fieldName, phraseContext, queryCustomizer, queryContext);
	}

	@Override
	public PhraseContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public PhraseContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public PhraseContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
