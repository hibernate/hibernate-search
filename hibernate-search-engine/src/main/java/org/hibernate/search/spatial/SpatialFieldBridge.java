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

	private int highGridLevel = MIN_GRID_LEVEL;
	private int lowGridLevel = MAX_GRID_LEVEL;

	private boolean gridIndex = true;
	private boolean numericFieldsIndex = true;

	public SpatialFieldBridge() {}

	public SpatialFieldBridge( int highGridLevel, int lowGridLevel ) {
		this.highGridLevel = highGridLevel;
		this.lowGridLevel = lowGridLevel;
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

			if( gridIndex ) {
				Point point = Point.fromDegrees( coordinates.getLatitude(), coordinates.getLongitude() );

				Map<Integer, String> cellIds = GridHelper.getGridCellsIds( point, highGridLevel, lowGridLevel );

				for ( int i = highGridLevel; i <= lowGridLevel; i++ ) {
					luceneOptions.addFieldToDocument( GridHelper.formatFieldName( i, name ), cellIds.get( i ), document );
				}
			}

			if( numericFieldsIndex ) {
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
	 * @param parameters Map containing the highGridLevel and lowGridLevel values
	 */
	@Override
	public void setParameterValues(Map parameters) {
		Object highGridLevel = parameters.get( "highGridLevel" );
		if ( highGridLevel instanceof Integer ) {
			this.highGridLevel = ( Integer ) highGridLevel;
		}
		Object lowGridLevel = parameters.get( "highGridLevel" );
		if ( lowGridLevel instanceof Integer ) {
			this.lowGridLevel = ( Integer ) lowGridLevel;
		}
		Object gridIndex = parameters.get( "gridIndex" );
		if ( gridIndex instanceof Boolean ) {
			this.gridIndex = ( Boolean ) gridIndex;
		}
		Object numericFieldsIndex = parameters.get( "numericFieldsIndex" );
		if ( numericFieldsIndex instanceof Boolean ) {
			this.numericFieldsIndex = ( Boolean ) numericFieldsIndex;
		}
	}
}