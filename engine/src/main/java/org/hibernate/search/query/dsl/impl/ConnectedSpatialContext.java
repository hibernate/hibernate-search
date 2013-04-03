/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.SpatialContext;
import org.hibernate.search.query.dsl.SpatialMatchingContext;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ConnectedSpatialContext implements SpatialContext {

	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;
	private final ConnectedQueryBuilder queryBuilder;

	public ConnectedSpatialContext(QueryBuildingContext context, ConnectedQueryBuilder queryBuilder) {
		this.queryContext = context;
		this.queryCustomizer = new QueryCustomizer();
		//today we only do constant score for spatial queries
		queryCustomizer.withConstantScore();
		spatialContext = new SpatialQueryContext();
		this.queryBuilder = queryBuilder;
	}

	@Override
	public SpatialMatchingContext onCoordinates(String field) {
		spatialContext.setCoordinatesField( field );
		return new ConnectedSpatialMatchingContext( queryContext, queryCustomizer, spatialContext, queryBuilder );
	}

	@Override
	public SpatialMatchingContext onDefaultCoordinates() {
		spatialContext.setDefaultCoordinatesField();
		return new ConnectedSpatialMatchingContext( queryContext, queryCustomizer, spatialContext, queryBuilder );
	}

	@Override
	public SpatialContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public SpatialContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public SpatialContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
