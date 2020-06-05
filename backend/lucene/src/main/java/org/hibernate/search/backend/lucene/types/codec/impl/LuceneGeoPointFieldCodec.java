/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.function.BiConsumer;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

public final class LuceneGeoPointFieldCodec implements LuceneFieldCodec<GeoPoint> {

	private final boolean projectable;
	private final boolean searchable;
	private final boolean sortable;

	private final GeoPoint indexNullAsValue;

	public LuceneGeoPointFieldCodec(boolean projectable, boolean searchable, boolean sortable, GeoPoint indexNullAsValue) {
		this.projectable = projectable;
		this.searchable = searchable;
		this.sortable = sortable;
		this.indexNullAsValue = indexNullAsValue;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, GeoPoint value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		if ( projectable ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, toStoredBytes( value ) ) );
		}

		if ( sortable || projectable ) {
			// The projectable term here is present only to support distance projections.
			// Since distances are derived from a DocValuesField, see DistanceCollector.
			documentBuilder.addField( new LatLonDocValuesField( absoluteFieldPath, value.latitude(), value.longitude() ) );
		}
		else {
			// For createExistsQuery()
			documentBuilder.addFieldName( absoluteFieldPath );
		}

		if ( searchable ) {
			documentBuilder.addField( new LatLonPoint( absoluteFieldPath, value.latitude(), value.longitude() ) );
		}
	}

	@Override
	public GeoPoint decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return fromStoredBytes( field.binaryValue() );
	}

	@Override
	public void contributeStoredFields(String absoluteFieldPath, String nestedDocumentPath,
			BiConsumer<String, String> collector) {
		collector.accept( absoluteFieldPath, nestedDocumentPath );
	}

	@Override
	public Query createExistsQuery(String absoluteFieldPath) {
		if ( sortable ) {
			return new DocValuesFieldExistsQuery( absoluteFieldPath );
		}
		else {
			return new TermQuery( new Term( MetadataFields.fieldNamesFieldName(), absoluteFieldPath ) );
		}
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

		return ( projectable == other.projectable ) && ( searchable == other.searchable )
				&& ( sortable == other.sortable );
	}

	private static BytesRef toStoredBytes(GeoPoint geoPoint) {
		byte[] bytes = new byte[2 * Double.BYTES];
		DoublePoint.encodeDimension( geoPoint.latitude(), bytes, 0 );
		DoublePoint.encodeDimension( geoPoint.longitude(), bytes, Double.BYTES );
		return new BytesRef( bytes );
	}

	private static GeoPoint fromStoredBytes(BytesRef bytesRef) {
		double latitude = DoublePoint.decodeDimension( bytesRef.bytes, bytesRef.offset );
		double longitude = DoublePoint.decodeDimension( bytesRef.bytes, bytesRef.offset + Double.BYTES );
		return GeoPoint.of( latitude, longitude );
	}
}
