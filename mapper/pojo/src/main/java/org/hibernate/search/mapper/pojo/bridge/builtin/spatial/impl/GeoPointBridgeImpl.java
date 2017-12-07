/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldAccessor;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;
import org.hibernate.search.engine.mapper.model.spi.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.spi.Bridge;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.spi.PojoState;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.StreamHelper;

/**
 * @author Yoann Rodiere
 */
public class GeoPointBridgeImpl implements Bridge {

	private final String fieldName;
	private final Store store;
	private final String markerSet;

	private IndexFieldAccessor<GeoPoint> fieldAccessor;
	private Function<PojoState, GeoPoint> coordinatesExtractor;

	public GeoPointBridgeImpl(String fieldName, Store store, String markerSet) {
		this.fieldName = fieldName;
		this.store = store;
		this.markerSet = markerSet;
	}

	@Override
	public void contribute(IndexSchemaElement indexSchemaElement, PojoModelElement bridgedPojoModelElement,
			SearchModel searchModel) {
		if ( fieldName == null || fieldName.isEmpty() ) {
			// TODO retrieve the default name somehow when parameters.name() is empty
			throw new UnsupportedOperationException( "Default field name not implemented yet" );
		}

		fieldAccessor = indexSchemaElement.field( fieldName ).asGeoPoint().createAccessor();

		if ( bridgedPojoModelElement.isAssignableTo( GeoPoint.class ) ) {
			PojoModelElementAccessor<GeoPoint> sourceAccessor = bridgedPojoModelElement.createAccessor( GeoPoint.class );
			coordinatesExtractor = sourceAccessor::read;
		}
		else {
			PojoModelElementAccessor<Double> latitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LatitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "latitude", fieldName, markerSet ) )
					.createAccessor( Double.class );
			PojoModelElementAccessor<Double> longitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LongitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "longitude", fieldName, markerSet ) )
					.createAccessor( Double.class );

			coordinatesExtractor = bridgedElement -> {
				Double latitude = latitudeAccessor.read( bridgedElement );
				Double longitude = longitudeAccessor.read( bridgedElement );

				if ( latitude == null || longitude == null ) {
					return null;
				}

				return new ImmutableGeoPoint( latitude, longitude );
			};
		}
	}

	private static Collector<PojoModelElement, ?, PojoModelElement> singleMarkedProperty(
			String markerName, String fieldName, String markerSet) {
		return StreamHelper.singleElement(
				() -> new SearchException( "Could not find a property with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" ),
				() -> new SearchException( "Found multiple properties with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" )
				);
	}

	@Override
	public void write(DocumentState target, PojoState source) {
		GeoPoint coordinates = coordinatesExtractor.apply( source );
		fieldAccessor.write( target, coordinates );
	}

}
