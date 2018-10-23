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
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.common.CollectionHelper;

public final class GeoPointFieldCodec implements LuceneFieldCodec<GeoPoint> {

	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";

	private final Store store;
	private final Sortable sortable;

	private final String latitudeAbsoluteFieldPath;
	private final String longitudeAbsoluteFieldPath;

	private final Set<String> storedFields;

	public GeoPointFieldCodec(String absoluteFieldPath, Store store, Sortable sortable) {
		this.store = store;
		this.sortable = sortable;

		switch ( store ) {
			case DEFAULT:
			case NO:
				latitudeAbsoluteFieldPath = null;
				longitudeAbsoluteFieldPath = null;
				storedFields = Collections.emptySet();
				break;
			case COMPRESS:
				// TODO HSEARCH-3081
			case YES:
				latitudeAbsoluteFieldPath = internalFieldName( absoluteFieldPath, LATITUDE );
				longitudeAbsoluteFieldPath = internalFieldName( absoluteFieldPath, LONGITUDE );
				storedFields = CollectionHelper.asSet( latitudeAbsoluteFieldPath, longitudeAbsoluteFieldPath );
				break;
			default: // The compiler wants a default entry, even if it doesn't make any sense
				throw new AssertionFailure( "Unexpected value for Store: " + store );
		}
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, GeoPoint value) {
		if ( value == null ) {
			return;
		}

		switch ( store ) {
			case DEFAULT:
			case NO:
				break;
			case YES:
				documentBuilder.addField( new StoredField( latitudeAbsoluteFieldPath, value.getLatitude() ) );
				documentBuilder.addField( new StoredField( longitudeAbsoluteFieldPath, value.getLongitude() ) );
				break;
			case COMPRESS:
				// TODO HSEARCH-3081
				break;
		}

		// the doc values field is also used when projecting on the distance
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
		if ( GeoPointFieldCodec.class != obj.getClass() ) {
			return false;
		}

		GeoPointFieldCodec other = (GeoPointFieldCodec) obj;

		return Objects.equals( store, other.store ) &&
				Objects.equals( sortable, other.sortable ) &&
				Objects.equals( latitudeAbsoluteFieldPath, other.latitudeAbsoluteFieldPath ) &&
				Objects.equals( longitudeAbsoluteFieldPath, other.longitudeAbsoluteFieldPath );
	}
}
