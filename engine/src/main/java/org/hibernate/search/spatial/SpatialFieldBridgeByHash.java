/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spatial.impl.SpatialNumericDocValueField;

/**
 * Hibernate Search field bridge, binding a Coordinates to a spatial hash field in the index
 *
 * @author Nicolas Helleringer
 */
public class SpatialFieldBridgeByHash extends SpatialFieldBridge implements ParameterizedBridge {

	public static final int DEFAULT_TOP_SPATIAL_HASH_LEVEL = 0;
	public static final int DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL = 16;

	private int topSpatialHashLevel = DEFAULT_TOP_SPATIAL_HASH_LEVEL;
	private int bottomSpatialHashLevel = DEFAULT_BOTTOM_SPATIAL_HASH_LEVEL;

	private boolean spatialHashIndex = true;
	private boolean numericFieldsIndex = true;

	private String[] hashIndexedFieldNames;

	public SpatialFieldBridgeByHash() {
	}

	public SpatialFieldBridgeByHash(int topSpatialHashLevel, int bottomSpatialHashLevel) {
		this.topSpatialHashLevel = topSpatialHashLevel;
		this.bottomSpatialHashLevel = bottomSpatialHashLevel;
	}

	public SpatialFieldBridgeByHash(int topSpatialHashLevel, int bottomSpatialHashLevel, String latitudeField, String longitudeField) {
		this.topSpatialHashLevel = topSpatialHashLevel;
		this.bottomSpatialHashLevel = bottomSpatialHashLevel;
		this.latitudeField = latitudeField;
		this.longitudeField = longitudeField;
	}

	@Override
	public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
		super.configureFieldMetadata( name, builder );
		if ( spatialHashIndex ) {
			for ( int i = topSpatialHashLevel; i <= bottomSpatialHashLevel; i++ ) {
				builder.field( hashIndexedFieldNames[i], FieldType.STRING );
			}
		}
		if ( numericFieldsIndex ) {
			builder.field( SpatialHelper.formatLatitude( name ), FieldType.DOUBLE );
			builder.field( SpatialHelper.formatLongitude( name ), FieldType.DOUBLE );
		}
	}

	@Override
	protected void initializeIndexedFieldNames(String fieldName) {
		super.initializeIndexedFieldNames( fieldName );

		hashIndexedFieldNames = new String[bottomSpatialHashLevel + 1];
		for ( int i = topSpatialHashLevel; i <= bottomSpatialHashLevel; i++ ) {
			hashIndexedFieldNames[i] = SpatialHelper.formatFieldName( i, fieldName );
		}
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

				if ( spatialHashIndex ) {
					Point point = Point.fromDegrees( latitude, longitude );

					for ( int i = topSpatialHashLevel; i <= bottomSpatialHashLevel; i++ ) {
						luceneOptions.addFieldToDocument( hashIndexedFieldNames[i], SpatialHelper.getSpatialHashCellId( point, i ), document );
					}
				}

				if ( numericFieldsIndex ) {
					luceneOptions.addNumericFieldToDocument(
							latitudeIndexedFieldName,
							latitude,
							document
					);

					luceneOptions.addNumericFieldToDocument(
							longitudeIndexedFieldName,
							longitude,
							document
					);

					Field latitudeDocValuesField = new SpatialNumericDocValueField( latitudeIndexedFieldName, latitude );
					document.add( latitudeDocValuesField );

					Field longitudeDocValuesField = new SpatialNumericDocValueField( longitudeIndexedFieldName, longitude );
					document.add( longitudeDocValuesField );
				}
			}
		}
	}

	/**
	 * Override method for default min and max spatial hash level
	 *
	 * @param parameters Map containing the topSpatialHashLevel and bottomSpatialHashLevel values
	 */
	@Override
	public void setParameterValues(final Map parameters) {
		Object topSpatialHashLevel = parameters.get( "topSpatialHashLevel" );
		if ( topSpatialHashLevel instanceof Integer ) {
			this.topSpatialHashLevel = (Integer) topSpatialHashLevel;
		}
		Object bottomSpatialHashLevel = parameters.get( "bottomSpatialHashLevel" );
		if ( bottomSpatialHashLevel instanceof Integer ) {
			this.bottomSpatialHashLevel = (Integer) bottomSpatialHashLevel;
		}
		Object spatialHashIndex = parameters.get( "spatialHashIndex" );
		if ( spatialHashIndex instanceof Boolean ) {
			this.spatialHashIndex = (Boolean) spatialHashIndex;
		}
		Object numericFieldsIndex = parameters.get( "numericFieldsIndex" );
		if ( numericFieldsIndex instanceof Boolean ) {
			this.numericFieldsIndex = (Boolean) numericFieldsIndex;
		}
	}
}
