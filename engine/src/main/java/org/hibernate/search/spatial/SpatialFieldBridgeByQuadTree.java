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

import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spatial.impl.Point;

import java.util.Map;

/**
 * Hibernate Search field bridge, binding a Coordinates to Quad Tree field in the index
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialFieldBridgeByQuadTree extends SpatialFieldBridge implements ParameterizedBridge {

	public static final int DEFAULT_TOP_QUAD_TREE_LEVEL = 0;
	public static final int DEFAULT_BOTTOM_QUAD_TREE_LEVEL = 16;

	private int topQuadTreeLevel = DEFAULT_TOP_QUAD_TREE_LEVEL;
	private int bottomQuadTreeLevel = DEFAULT_BOTTOM_QUAD_TREE_LEVEL;

	private boolean quadTreeIndex = true;
	private boolean numericFieldsIndex = true;

	public SpatialFieldBridgeByQuadTree() {
	}

	public SpatialFieldBridgeByQuadTree(int topQuadTreeLevel, int bottomQuadTreeLevel) {
		this.topQuadTreeLevel = topQuadTreeLevel;
		this.bottomQuadTreeLevel = bottomQuadTreeLevel;
	}

	public SpatialFieldBridgeByQuadTree(int topQuadTreeLevel, int bottomQuadTreeLevel, String latitudeField, String longitudeField) {
		this.topQuadTreeLevel = topQuadTreeLevel;
		this.bottomQuadTreeLevel = bottomQuadTreeLevel;
		this.latitudeField = latitudeField;
		this.longitudeField = longitudeField;
	}

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

			Double latitude = getLatitude( value );
			Double longitude = getLongitude( value );

			if ( ( latitude != null ) && ( longitude != null ) ) {

				if ( quadTreeIndex ) {
					Point point = Point.fromDegrees( latitude, longitude );

					for ( int i = topQuadTreeLevel; i <= bottomQuadTreeLevel; i++ ) {
						luceneOptions.addFieldToDocument( SpatialHelper.formatFieldName( i, name ), SpatialHelper.getQuadTreeCellId( point, i ), document );
					}
				}

				if ( numericFieldsIndex ) {
					luceneOptions.addNumericFieldToDocument(
							SpatialHelper.formatLatitude( name ),
							latitude,
							document
					);

					luceneOptions.addNumericFieldToDocument(
							SpatialHelper.formatLongitude( name ),
							longitude,
							document
					);
				}
			}
		}
	}

	/**
	 * Override method for default min and max quad tree level
	 *
	 * @param parameters Map containing the topQuadTreeLevel and bottomQuadTreeLevel values
	 */
	@Override
	public void setParameterValues(final Map parameters) {
		Object topQuadTreeLevel = parameters.get( "topQuadTreeLevel" );
		if ( topQuadTreeLevel instanceof Integer ) {
			this.topQuadTreeLevel = (Integer) topQuadTreeLevel;
		}
		Object bottomQuadTreeLevel = parameters.get( "bottomQuadTreeLevel" );
		if ( bottomQuadTreeLevel instanceof Integer ) {
			this.bottomQuadTreeLevel = (Integer) bottomQuadTreeLevel;
		}
		Object quadTreeIndex = parameters.get( "quadTreeIndex" );
		if ( quadTreeIndex instanceof Boolean ) {
			this.quadTreeIndex = (Boolean) quadTreeIndex;
		}
		Object numericFieldsIndex = parameters.get( "numericFieldsIndex" );
		if ( numericFieldsIndex instanceof Boolean ) {
			this.numericFieldsIndex = (Boolean) numericFieldsIndex;
		}
	}
}
