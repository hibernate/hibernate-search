/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import org.apache.lucene.search.SortField;

import org.hibernate.search.spatial.impl.DistanceComparatorSource;
import org.hibernate.search.spatial.impl.Point;

/**
 * Lucene SortField for sorting documents which have been indexed with Hibernate Search spatial
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByQuadTree
 * @see org.hibernate.search.spatial.SpatialFieldBridgeByRange
 * @see org.hibernate.search.spatial.Coordinates
 */
public class DistanceSortField extends SortField {

	public DistanceSortField(Coordinates center, String fieldName) {
		super( fieldName, new DistanceComparatorSource( center ) );
	}

	public DistanceSortField(double latitude, double longitude, String fieldName) {
		this( Point.fromDegrees( latitude, longitude ), fieldName );
	}
}
