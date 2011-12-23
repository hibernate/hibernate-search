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
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.spatial.impl.GridHelper;
import org.hibernate.search.spatial.impl.Point;

import java.util.Map;

/**
 * Hibernate Search field bridge, binding a Coordinates to Grid field in the index
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialFieldBridge implements FieldBridge, ParameterizedBridge {

	public static final int MIN_GRID_LEVEL = 0;
	public static final int MAX_GRID_LEVEL = 16;

	private int min_grid_level = MIN_GRID_LEVEL;
	private int max_grid_level = MAX_GRID_LEVEL;

	private boolean grid_index = true;
	private boolean numeric_fields_index = true;

	public SpatialFieldBridge() {}

	public SpatialFieldBridge( int min_grid_level, int max_grid_level ) {
		this.min_grid_level= min_grid_level;
		this.max_grid_level= max_grid_level;
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

			Coordinates coordinates = (Coordinates) value;

			if( grid_index ) {
				Point point = Point.fromDegrees( coordinates.getLatitude(), coordinates.getLongitude() );

				Map<Integer, String> cellIds = GridHelper.getGridCellsIds( point, min_grid_level, max_grid_level );

				for ( int i = min_grid_level; i <= max_grid_level; i++ ) {
					luceneOptions.addFieldToDocument( GridHelper.formatFieldName( i, name ), cellIds.get( i ), document );
				}
			}

			if( numeric_fields_index ) {
				luceneOptions.addNumericFieldToDocument(
						GridHelper.formatLatitude( name ),
						coordinates.getLatitude(),
						document
				);

				luceneOptions.addNumericFieldToDocument(
						GridHelper.formatLongitude( name ),
						coordinates.getLongitude(),
						document
				);
			}
		}
	}

	/**
	 * Override method for default min and max grid level
	 *
	 * @param parameters Map containing the min_grid_level and max_grid_level values
	 */
	@Override
	public void setParameterValues(Map parameters) {
		Object min_grid_level = parameters.get( "min_grid_level" );
		if ( min_grid_level instanceof Integer ) {
			this.min_grid_level = ( Integer ) min_grid_level;
		}
		Object max_grid_level = parameters.get( "min_grid_level" );
		if ( max_grid_level instanceof Integer ) {
			this.max_grid_level = ( Integer ) max_grid_level;
		}
		Object grid_index = parameters.get( "grid_index" );
		if ( grid_index instanceof Boolean ) {
			this.grid_index = ( Boolean ) grid_index;
		}
		Object numeric_fields_index = parameters.get( "numeric_fields_index" );
		if ( numeric_fields_index instanceof Boolean ) {
			this.numeric_fields_index = ( Boolean ) numeric_fields_index;
		}
	}
}