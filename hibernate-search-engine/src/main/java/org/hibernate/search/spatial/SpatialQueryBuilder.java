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
package org.hibernate.search.spatial;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;

import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.SpatialQueryBuilderFromPoint;

/**
 * The SpatialQueryBuilder hold builder methods for Grid, Distance and Spatial (Grid+Distance) filters and queries
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public abstract class SpatialQueryBuilder {
	/**
	 * Returns a Lucene Query which rely on Hibernate Search Spatial
	 * grid indexation to filter document at radius by wrapping a
	 * GridFilter into a ConstantScoreQuery
	 *
	 * @param latitude WGS84 latitude of the center of the search 
	 * @param longitude WGS84 longitude of the center of the search 
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see	Query
	 * @see	Coordinates
	 * @see ConstantScoreQuery
	 */
	public static Query buildGridQuery(double latitude, double longitude, double radius, String fieldName) {
		return SpatialQueryBuilderFromPoint.buildGridQuery(
				Point.fromDegrees( latitude, longitude ),
				radius,
				fieldName
		);
	}

	/**
	 * Returns a Lucene Query searching directly by conmputing distance against
	 * all docs in the index (costly !)
	 *
	 * @param latitude WGS84 latitude of the center of the search 
	 * @param longitude WGS84 longitude of the center of the search 
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see	Query
	 * @see	Coordinates
	 */
	public static Query buildDistanceQuery(double latitude, double longitude, double radius, String fieldName) {
		return SpatialQueryBuilderFromPoint.buildDistanceQuery(
				Point.fromDegrees( latitude, longitude ),
				radius,
				fieldName
		);
	}

	/**
	 * Returns a Lucene Query which rely on Hibernate Search Spatial
	 * grid indexation to filter document at radius and filter its results
	 * by a fine DistanceFilter
	 *
	 * @param latitude WGS84 latitude of the center of the search 
	 * @param longitude WGS84 longitude of the center of the search 
	 * @param radius distance max to center in km
	 * @param fieldName name of the Lucene Field implementing Coordinates
	 *
	 * @return Lucene Query to be used in a search
	 *
	 * @see	Query
	 * @see	Coordinates
	 */
	public static Query buildSpatialQuery(double latitude, double longitude, double radius, String fieldName) {
		return SpatialQueryBuilderFromPoint.buildSpatialQuery(
				Point.fromDegrees( latitude, longitude ),
				radius,
				fieldName
		);
	}
}
