/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.spi.DocumentState;
import org.hibernate.search.engine.backend.document.spi.IndexFieldAccessor;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridge;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;
import org.hibernate.search.mapper.pojo.bridge.spi.Bridge;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElement;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementReader;
import org.hibernate.search.mapper.pojo.model.spi.BridgedElementModel;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.StreamHelper;

/**
 * @author Yoann Rodiere
 */
public class GeoPointBridgeImpl implements Bridge<GeoPointBridge> {

	private GeoPointBridge parameters;

	private IndexFieldAccessor<GeoPoint> fieldAccessor;
	private Function<BridgedElement, GeoPoint> coordinatesExtractor;

	@Override
	public void initialize(BuildContext buildContext, GeoPointBridge parameters) {
		this.parameters = parameters;
	}

	@Override
	public void bind(IndexSchemaElement indexSchemaElement, BridgedElementModel bridgedElementModel) {
		String fieldName = parameters.fieldName();

		if ( fieldName.isEmpty() ) {
			// TODO retrieve the default name somehow when parameters.name() is empty
			throw new UnsupportedOperationException( "Default field name not implemented yet" );
		}

		fieldAccessor = indexSchemaElement.field( fieldName ).asGeoPoint().createAccessor();

		if ( bridgedElementModel.isAssignableTo( GeoPoint.class ) ) {
			BridgedElementReader<GeoPoint> sourceReference = bridgedElementModel.createReader( GeoPoint.class );
			coordinatesExtractor = sourceReference::read;
		}
		else {
			String markerSet = parameters.markerSet();

			BridgedElementReader<Double> latitudeReference = bridgedElementModel.properties()
					.filter( model -> model.markers( GeoPointBridge.Latitude.class )
							.anyMatch( m -> markerSet.equals( m.markerSet() ) ) )
					.collect( singleMarkedProperty( "latitude", fieldName, markerSet ) )
					.createReader( Double.class );
			BridgedElementReader<Double> longitudeReference = bridgedElementModel.properties()
					.filter( model -> model.markers( GeoPointBridge.Longitude.class )
							.anyMatch( m -> markerSet.equals( m.markerSet() ) ) )
					.collect( singleMarkedProperty( "longitude", fieldName, markerSet ) )
					.createReader( Double.class );

			coordinatesExtractor = bridgedElement -> {
				Double latitude = latitudeReference.read( bridgedElement );
				Double longitude = longitudeReference.read( bridgedElement );

				if ( latitude == null || longitude == null ) {
					return null;
				}

				return new ImmutableGeoPoint( latitude, longitude );
			};
		}
	}

	private static Collector<BridgedElementModel, ?, BridgedElementModel> singleMarkedProperty(
			String markerName, String fieldName, String markerSet) {
		return StreamHelper.singleElement(
				() -> new SearchException( "Could not find a property with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" ),
				() -> new SearchException( "Found multiple properties with the " + markerName
						+ " marker for field '" + fieldName + "' (marker set: '" + markerSet + "')" )
				);
	}

	@Override
	public void write(DocumentState target, BridgedElement source) {
		GeoPoint coordinates = coordinatesExtractor.apply( source );
		fieldAccessor.write( target, coordinates );
	}

}
