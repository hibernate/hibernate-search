/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

public final class LuceneGeoPointFieldCodec implements LuceneFieldCodec<GeoPoint> {

	private final Indexing indexing;
	private final DocValues docValues;
	private final Storage storage;
	private final GeoPoint indexNullAsValue;

	public LuceneGeoPointFieldCodec(Indexing indexing, DocValues docValues, Storage storage,
			GeoPoint indexNullAsValue) {
		this.indexing = indexing;
		this.docValues = docValues;
		this.storage = storage;
		this.indexNullAsValue = indexNullAsValue;
	}

	@Override
	public void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, GeoPoint value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		if ( Indexing.ENABLED == indexing ) {
			documentBuilder.addField( new LatLonPoint( absoluteFieldPath, value.latitude(), value.longitude() ) );
		}

		if ( DocValues.ENABLED == docValues ) {
			documentBuilder.addField( new LatLonDocValuesField( absoluteFieldPath, value.latitude(), value.longitude() ) );
		}
		else {
			// For the "exists" predicate
			documentBuilder.addFieldName( absoluteFieldPath );
		}

		if ( Storage.ENABLED == storage ) {
			documentBuilder.addField( new StoredField( absoluteFieldPath, toStoredBytes( value ) ) );
		}
	}

	@Override
	public GeoPoint decode(IndexableField field) {
		return fromStoredBytes( field.binaryValue() );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		return LuceneGeoPointFieldCodec.class == obj.getClass();
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
