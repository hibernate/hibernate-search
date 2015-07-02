/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spatial.impl.SpatialNumericDocValueField;

/**
 * Hibernate Search field bridge using Range Spatial, binding a Coordinates to two numeric fields for latitude and Longitude
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialFieldBridgeByRange extends SpatialFieldBridge {

	public SpatialFieldBridgeByRange() {
	}

	public SpatialFieldBridgeByRange(String latitudeField, String longitudeField) {
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

				Field latitudeDocValuesField = new SpatialNumericDocValueField( SpatialHelper.formatLatitude( name ), getLatitude( value ) );
				document.add( latitudeDocValuesField );

				Field longitudeDocValuesField = new SpatialNumericDocValueField( SpatialHelper.formatLongitude( name ), getLongitude( value ) );
				document.add( longitudeDocValuesField );
			}
		}
	}
}
