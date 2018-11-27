/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static org.hibernate.search.backend.lucene.util.impl.LuceneFields.internalFieldName;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.CollectionHelper;

public final class LuceneGeoPointFieldCodec implements LuceneFieldCodec<GeoPoint> {

	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";

	private final boolean projectable;
	private final boolean sortable;

	private final String latitudeAbsoluteFieldPath;
	private final String longitudeAbsoluteFieldPath;

	private final Set<String> storedFields;

	public LuceneGeoPointFieldCodec(String absoluteFieldPath, boolean projectable, boolean sortable) {
		this.projectable = projectable;
		this.sortable = sortable;

		if ( projectable ) {
			latitudeAbsoluteFieldPath = internalFieldName( absoluteFieldPath, LATITUDE );
			longitudeAbsoluteFieldPath = internalFieldName( absoluteFieldPath, LONGITUDE );
			storedFields = CollectionHelper.asSet( latitudeAbsoluteFieldPath, longitudeAbsoluteFieldPath );
		}
		else {
			latitudeAbsoluteFieldPath = null;
			longitudeAbsoluteFieldPath = null;
			storedFields = Collections.emptySet();
		}
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, GeoPoint value) {
		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( latitudeAbsoluteFieldPath, value.getLatitude() ) );
			documentBuilder.addField( new StoredField( longitudeAbsoluteFieldPath, value.getLongitude() ) );
		}

		// doc values fields are required for predicates, distance projections and distance sorts
		documentBuilder.addField( new LatLonDocValuesField( absoluteFieldPath, value.getLatitude(), value.getLongitude() ) );
		documentBuilder.addField( new LatLonPoint( absoluteFieldPath, value.getLatitude(), value.getLongitude() ) );
	}

	@Override
	public GeoPoint decode(Document document, String absoluteFieldPath) {
		IndexableField latitudeField = document.getField( latitudeAbsoluteFieldPath );
		IndexableField longitudeField = document.getField( longitudeAbsoluteFieldPath );

		if ( latitudeField == null || longitudeField == null ) {
			return null;
		}

		return GeoPoint.of( (double) latitudeField.numericValue(), (double) longitudeField.numericValue() );
	}

	@Override
	public Set<String> getOverriddenStoredFields() {
		return storedFields;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneGeoPointFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneGeoPointFieldCodec other = (LuceneGeoPointFieldCodec) obj;

		return ( projectable == other.projectable ) &&
				( sortable == other.sortable ) &&
				Objects.equals( latitudeAbsoluteFieldPath, other.latitudeAbsoluteFieldPath ) &&
				Objects.equals( longitudeAbsoluteFieldPath, other.longitudeAbsoluteFieldPath );
	}
}
