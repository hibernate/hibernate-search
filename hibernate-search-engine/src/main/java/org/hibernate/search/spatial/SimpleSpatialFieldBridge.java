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

import org.apache.lucene.document.Document;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Hibernate Search Simple Spatial field bridge, binding a Coordinates to two numeric fields for latitude and Longitude
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SimpleSpatialFieldBridge implements FieldBridge {

	public SimpleSpatialFieldBridge() {}

	/**
	 * Actual overridden method that does the indexing
	 *
	 * @param name of the field
	 * @param value of the field
	 * @param document document being indexed
	 * @param luceneOptions current indexing options and accessors
	 */
	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		if ( value != null ) {

			Coordinates coordinates = (Coordinates) value;

			luceneOptions.addNumericFieldToDocument(
					"latitude",
					coordinates.getLatitude(),
					document
			);

			luceneOptions.addNumericFieldToDocument(
					"longitude",
					coordinates.getLongitude(),
					document
			);

		}
	}
}