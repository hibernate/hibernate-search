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

import org.hibernate.search.query.dsl.AllContext;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.FacetContext;
import org.hibernate.search.query.dsl.PhraseContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.TermContext;

/**
 * Assuming connection with the search factory
 *
 * @author Emmanuel Bernard
 */
public class ConnectedQueryBuilder implements QueryBuilder {
	private final QueryBuildingContext context;

	public ConnectedQueryBuilder(QueryBuildingContext context) {
		this.context = context;
	}

	@Override
	public TermContext keyword() {
		return new ConnectedTermContext( context );
	}

	@Override
	public RangeContext range() {
		return new ConnectedRangeContext( context );
	}

	@Override
	public PhraseContext phrase() {
		return new ConnectedPhraseContext( context );
	}

	//fixme Have to use raw types but would be nice to not have to
	@Override
	public BooleanJunction bool() {
		return new BooleanQueryBuilder();
	}

	@Override
	public AllContext all() {
		return new ConnectedAllContext();
	}

	@Override
	public FacetContext facet() {
		return new ConnectedFacetContext( new FacetBuildingContext( context.getFactory(), context.getEntityType() ) );
	}

	@Override
	public SpatialContext spatial() {
		return new ConnectedSpatialContext( context, this );
	}
}
